package com.chm.aiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.chm.aiagent.agent.model.AgentState;
import com.chm.aiagent.exception.AgentException;
import com.chm.aiagent.exception.ErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM 大模型
    private ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new AgentException(ErrorCode.AGENT_RUNNING,
                    "Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new AgentException(ErrorCode.AGENT_EMPTY_PROMPT);
        }
        // 2、执行，更改状态
        this.state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时

        // 客户端断开标记，volatile 保证跨线程可见
        final boolean[] clientDisconnected = {false};

        // 设置完成回调（正常结束 / 客户端断开都会触发）
        sseEmitter.onCompletion(() -> {
            clientDisconnected[0] = true;
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            clientDisconnected[0] = true;
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 1、基础校验
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send(SseEmitter.event().name("error").data(
                            ErrorCode.AGENT_RUNNING.getMessage() + ": " + this.state));
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send(SseEmitter.event().name("error").data(
                            ErrorCode.AGENT_EMPTY_PROMPT.getMessage()));
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
                return;
            }
            // 2、执行，更改状态
            this.state = AgentState.RUNNING;
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));
            // 保存结果列表
            List<String> results = new ArrayList<>();
            try {
                // 执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    // 客户端已断开，不再调用 LLM
                    if (clientDisconnected[0]) {
                        log.info("客户端已断开，终止 Agent 循环");
                        break;
                    }

                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    // 单步执行（调用 LLM）
                    String stepResult = step();

                    // step() 之后再次检查，避免刚调完 LLM 就发送给已断开的客户端
                    if (clientDisconnected[0]) {
                        log.info("客户端已断开，跳过 SSE 发送");
                        break;
                    }

                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);
                    log.info(result);

                    // 发送 LLM 的真实回复文本，区分思考过程和最终回答
                    String thinkText = getLastThinkText();
                    if (StrUtil.isNotBlank(thinkText)) {
                        if (isLastThinkAnswer()) {
                            // 最终回答 → event:answer，前端直接展示
                            sseEmitter.send(SseEmitter.event().name("answer").data(thinkText));
                        } else {
                            // 内部思考/工具调用推理 → event:thinking，前端放入折叠区域
                            sseEmitter.send(SseEmitter.event().name("thinking").data(thinkText));
                            // 只展示简短的工具执行状态，不暴露原始返回数据
                            if (StrUtil.isNotBlank(stepResult) && !stepResult.equals("思考完成 - 无需行动")) {
                                sseEmitter.send(SseEmitter.event().name("thinking").data("\n🔧 " + stepResult));
                            }
                        }
                    } else {
                        sseEmitter.send(SseEmitter.event().name("answer").data(result));
                    }
                }
                // 正常完成（未断开时）
                if (!clientDisconnected[0]) {
                    sseEmitter.complete();
                }
            } catch (IOException e) {
                // SSE 发送时客户端断开，正常终止
                clientDisconnected[0] = true;
                log.info("SSE 发送失败，客户端已断开: {}", e.getMessage());
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                if (!clientDisconnected[0]) {
                    try {
                        sseEmitter.send("执行错误：" + e.getMessage());
                        sseEmitter.complete();
                    } catch (IOException ex) {
                        // 发送错误信息时也断开了，忽略
                    }
                }
            } finally {
                // 3、清理资源
                this.cleanup();
            }
        });

        return sseEmitter;
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 获取最近一次思考中 LLM 返回的文本内容（供 SSE 流式输出使用）
     * 默认返回 null，子类（如 ToolCallAgent）可重写
     */
    public String getLastThinkText() {
        return null;
    }

    /**
     * 判断最近一次 think() 是否是给用户的最终回答
     * 默认 true（普通 Agent 无工具调用，所有输出都是回答）
     * ToolCallAgent 会根据是否有工具调用来区分
     */
    public boolean isLastThinkAnswer() {
        return true;
    }

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }
}
