package com.chm.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.chm.aiagent.agent.fallback.ToolFailureContext;
import com.chm.aiagent.agent.fallback.ToolFallbackHandler;
import com.chm.aiagent.agent.fallback.ToolFallbackProperties;
import com.chm.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 工具调用失败兜底处理器（由 Spring 注入）
    private final ToolFallbackHandler fallbackHandler;

    // 工具失败兜底配置
    private final ToolFallbackProperties fallbackProperties;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    // 保存最近一次 think() 中 LLM 返回的文本内容，供 SSE 流式输出
    private String lastThinkText;

    // 跟踪上一轮是否有工具调用，用于判断是否需要追加 nextStepPrompt
    private boolean lastHadToolCalls = false;

    public ToolCallAgent(ToolCallback[] availableTools,
                          ToolFallbackHandler fallbackHandler,
                          ToolFallbackProperties fallbackProperties) {
        super();
        this.availableTools = availableTools;
        this.fallbackHandler = fallbackHandler;
        this.fallbackProperties = fallbackProperties;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、只在首次调用或上一轮有工具调用时，才追加 nextStepPrompt 引导下一步
        //    避免 LLM 已返回纯文本（任务完成）后仍反复追加提示词导致循环输出
        if (StrUtil.isNotBlank(getNextStepPrompt()) && (getCurrentStep() == 0 || lastHadToolCalls)) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            // 如果有 SSE 通道则走流式输出（逐 token 推送），否则走同步调用
            if (getStreamEmitter() != null) {
                return thinkWithStream(prompt);
            }
            return thinkSync(prompt);
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            this.lastHadToolCalls = false;
            setState(AgentState.ERROR);
            return false;
        }
    }

    /** 同步模式：等 LLM 整段生成完再返回（run() 方法使用），含指数退避重试 */
    private boolean thinkSync(Prompt prompt) {
        int maxRetries = fallbackProperties.getLlm().getMaxRetries();
        List<Long> backoffMs = fallbackProperties.getLlm().getBackoffMs();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ChatResponse chatResponse = getChatClient().prompt(prompt)
                        .system(getSystemPrompt())
                        .tools(availableTools)
                        .call()
                        .chatResponse();
                return processThinkResult(chatResponse, chatResponse.getResult().getOutput().getText());
            } catch (Exception e) {
                if (attempt >= maxRetries || !fallbackHandler.isRetryableException(e)) {
                    log.error("{} LLM 调用失败（不可重试或重试耗尽），attempt={}/{}", getName(), attempt, maxRetries);
                    throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
                }
                long waitMs = attempt < backoffMs.size() ? backoffMs.get(attempt) : backoffMs.get(backoffMs.size() - 1);
                log.warn("{} LLM 调用失败（第 {}/{} 次），{}ms 后重试: {}", getName(), attempt + 1, maxRetries, waitMs, e.getMessage());
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            }
        }
        return false; // unreachable
    }

    /** 流式模式：逐 token 推送 SSE，含 LLM 指数退避重试 */
    private boolean thinkWithStream(Prompt prompt) {
        int maxRetries = fallbackProperties.getLlm().getMaxRetries();
        List<Long> backoffMs = fallbackProperties.getLlm().getBackoffMs();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            StringBuilder fullText = new StringBuilder();
            ChatResponse[] lastResponse = {null};
            Throwable[] streamError = {null};

            try {
                getChatClient().prompt(prompt)
                        .system(getSystemPrompt())
                        .tools(availableTools)
                        .stream()
                        .chatResponse()
                        .doOnNext(chatResponse -> {
                            String text = chatResponse.getResult().getOutput().getText();
                            if (text != null) {
                                fullText.append(text);
                                try {
                                    getStreamEmitter().send(SseEmitter.event().name("thinking").data(text));
                                } catch (IOException e) {
                                    log.info("SSE 发送失败，客户端可能已断开");
                                }
                            }
                            lastResponse[0] = chatResponse;
                        })
                        .doOnError(e -> {
                            streamError[0] = e;
                            log.error(getName() + " 流式调用出错：" + e.getMessage());
                        })
                        .blockLast();

                // 流式调用成功
                if (lastResponse[0] != null) {
                    return processThinkResult(lastResponse[0], fullText.toString());
                }

                // 流未返回结果，检查是否有错误可重试
                if (streamError[0] != null) {
                    throw new RuntimeException("流式调用失败", streamError[0]);
                }

                // 无错误但无结果：空响应
                log.error(getName() + " 流式调用未返回任何结果");
                this.lastThinkText = fullText.toString();
                this.lastHadToolCalls = false;
                setState(AgentState.ERROR);
                return false;

            } catch (Exception e) {
                // 检查是否可重试
                Throwable root = e.getCause() != null ? e.getCause() : e;
                if (attempt < maxRetries && fallbackHandler.isRetryableException(root)) {
                    long waitMs = attempt < backoffMs.size() ? backoffMs.get(attempt) : backoffMs.get(backoffMs.size() - 1);
                    log.warn("{} LLM 流式调用失败（第 {}/{} 次），{}ms 后重试: {}",
                            getName(), attempt + 1, maxRetries, waitMs, root.getMessage());
                    // 通知前端正在重试
                    try {
                        if (getStreamEmitter() != null) {
                            getStreamEmitter().send(SseEmitter.event().name("thinking")
                                    .data("\n🔄 AI 响应超时，正在重试（" + (attempt + 1) + "/" + maxRetries + "）...\n"));
                        }
                    } catch (IOException ignored) {}
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue; // 重试
                }

                // 不可重试或重试耗尽
                log.error(getName() + " 流式调用最终失败（attempt={}/{}）：{}", attempt, maxRetries, e.getMessage());
                getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
                this.lastHadToolCalls = false;
                this.lastThinkText = fullText.length() > 0 ? fullText.toString() : null;
                setState(AgentState.ERROR);
                return false;
            }
        }

        // 所有重试耗尽
        setState(AgentState.ERROR);
        return false;
    }

    /** 统一处理 LLM 返回结果：解析 ToolCall 列表并决定是否需要 act() */
    private boolean processThinkResult(ChatResponse chatResponse, String fullText) {
        this.toolCallChatResponse = chatResponse;
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
        this.lastThinkText = fullText;

        log.info(getName() + "的思考：" + fullText);
        log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
        String toolCallInfo = toolCallList.stream()
                .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                .collect(Collectors.joining("\n"));
        log.info(toolCallInfo);

        if (toolCallList.isEmpty()) {
            getMessageList().add(assistantMessage);
            this.lastHadToolCalls = false;
            setState(AgentState.FINISHED);
            return false;
        } else {
            this.lastHadToolCalls = true;
            return true;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
        }
        // 完整结果写入日志（供排查）
        String fullResults = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(fullResults);

        // ====== 工具调用失败检测与多层兜底（五层架构的 Layer 1-4） ======
        Map<String, String> toolArgsMap = extractToolArgs();
        List<ToolFailureContext> failures = fallbackHandler.detectFailures(
                toolResponseMessage, toolArgsMap,
                getDegradationRound(), getCircuitBrokenTools(), getToolFailureCount());

        if (!failures.isEmpty() && !terminateToolCalled) {
            // 记录失败次数
            for (ToolFailureContext f : failures) {
                recordToolFailure(f.getToolName());
                int failCount = getToolFailureCount().get(f.getToolName());
                // 检查是否需要触发熔断
                if (failCount >= fallbackProperties.getCircuitBreaker().getFailureThreshold()) {
                    markCircuitBroken(f.getToolName());
                    log.warn("🔌 工具 [{}] 触发熔断（连续失败 {} 次），本对话中不再建议调用",
                            f.getToolName(), failCount);
                }
            }

            // 判断降级轮次是否耗尽
            int maxRounds = fallbackProperties.getDegradation().getMaxRounds();
            int remainingRounds = maxRounds - getDegradationRound();

            if (remainingRounds > 0) {
                // Layer 2-3：能力域降级 + LLM 自主决策
                incrementDegradationRound();
                String fallbackMsg = fallbackHandler.buildFallbackMessage(failures, remainingRounds - 1);
                getMessageList().add(new UserMessage(fallbackMsg));
                log.info("📋 注入降级上下文（第 {}/{} 轮，剩余 {} 轮），共 {} 个工具失败",
                        getDegradationRound(), maxRounds, remainingRounds - 1, failures.size());

                // SSE 通知前端
                sendToolFallbackSSE(failures, false);
            } else {
                // Layer 4：降级耗尽，引导 LLM 最终回复
                String finalMsg = fallbackHandler.buildFinalFallbackMessage(failures);
                getMessageList().add(new UserMessage(finalMsg));
                log.warn("🛑 降级轮次耗尽（{}/{}），注入最终兜底消息，引导 LLM 文本回复",
                        getDegradationRound(), maxRounds);

                // SSE 通知前端
                sendToolFallbackSSE(failures, true);
            }
        }
        // ====== 兜底逻辑结束 ======

        // 返回用户友好的简短状态，不暴露原始数据
        String toolNames = toolResponseMessage.getResponses().stream()
                .map(ToolResponseMessage.ToolResponse::name)
                .map(this::friendlyToolStatus)
                .distinct()
                .collect(Collectors.joining("、"));
        return toolNames;
    }

    /** 将工具名映射为用户友好的状态描述 */
    private String friendlyToolStatus(String toolName) {
        if (toolName.contains("earch") || toolName.contains("Search")) return "正在搜索相关信息";
        if (toolName.contains("crap") || toolName.contains("Scrap")) return "正在分析网页内容";
        if (toolName.contains("ownload") || toolName.contains("Download")) return "正在下载资源";
        if (toolName.contains("ead") || toolName.contains("rite")) return "正在处理文件";
        if (toolName.contains("erminal") || toolName.contains("mmand")) return "正在执行系统命令";
        if (toolName.contains("PDF") || toolName.contains("pdf")) return "正在生成文档";
        if (toolName.contains("erminate")) return "任务完成";
        if (toolName.contains("eather") || toolName.contains("Weather")) return "正在查询天气信息";
        return "正在处理中";
    }

    /**
     * 获取最近一次 think() 中 LLM 返回的文本内容
     */
    public String getLastThinkText() {
        return lastThinkText;
    }

    /**
     * 判断最近一次 think() 是否是给用户的最终回答（无工具调用）
     * true  → 最终回答，应直接展示给用户
     * false → 内部思考/推理，应放入折叠的思考过程区域
     */
    @Override
    public boolean isLastThinkAnswer() {
        return !lastHadToolCalls;
    }

    // ========== 工具失败兜底辅助方法 ==========

    /** 从 toolCallChatResponse 中提取 toolName → args JSON 的映射 */
    private Map<String, String> extractToolArgs() {
        Map<String, String> map = new LinkedHashMap<>();
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return map;
        }
        AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            map.put(toolCall.name(), toolCall.arguments());
        }
        return map;
    }

    /** 通过 SSE 通知前端工具失败/降级状态 */
    private void sendToolFallbackSSE(List<ToolFailureContext> failures, boolean isFinal) {
        if (getStreamEmitter() == null) return;
        try {
            if (isFinal) {
                getStreamEmitter().send(SseEmitter.event().name("thinking")
                        .data("\n⚠️ 多次尝试失败，正在为你生成最终回复...\n"));
            } else {
                String toolNames = failures.stream()
                        .map(ToolFailureContext::getToolName)
                        .distinct()
                        .collect(Collectors.joining("、"));
                getStreamEmitter().send(SseEmitter.event().name("thinking")
                        .data("\n⚠️ " + toolNames + " 执行失败，正在尝试备用方案...\n"));
            }
        } catch (IOException e) {
            log.info("SSE 发送失败，客户端可能已断开");
        }
    }
}