package com.chm.aiagent.exception;

/**
 * Agent 执行异常
 * <p>
 * 用于封装 Agent 在 think / act / step 生命周期中的各类错误，
 * 包括 LLM 调用失败、工具执行异常、状态机非法转换等。
 */
public class AgentException extends BusinessException {

    public AgentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AgentException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public AgentException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public AgentException(String agentName, ErrorCode errorCode, Throwable cause) {
        super(errorCode, String.format("[%s] %s", agentName, cause.getMessage()), cause);
    }
}
