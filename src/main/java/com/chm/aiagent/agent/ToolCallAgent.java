package com.chm.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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

import java.util.List;
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

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
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
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();

            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            this.lastThinkText = result;
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                // 没有工具需要调用，任务自然结束
                getMessageList().add(assistantMessage);
                this.lastHadToolCalls = false;
                setState(AgentState.FINISHED);
                return false;
            } else {
                // 需要调用工具时，无需手动记录助手消息，act() 中会自动记录
                this.lastHadToolCalls = true;
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            this.lastHadToolCalls = false;
            setState(AgentState.ERROR);
            return false;
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
}