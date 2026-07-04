package com.chm.aiagent.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>
 * 格式：模块前缀(2位) + 错误序号(3位)
 * 模块：10=通用, 20=Agent, 30=工具, 40=SSE
 */
@Getter
public enum ErrorCode {

    // ========== 通用 10xxx ==========
    SUCCESS(0, "ok"),
    BAD_REQUEST(10001, "请求参数错误"),
    VALIDATION_ERROR(10002, "参数校验失败"),
    INTERNAL_ERROR(10003, "服务器内部错误"),
    NOT_FOUND(10004, "资源不存在"),

    // ========== Agent 20xxx ==========
    AGENT_RUNNING(20001, "Agent 正在运行中"),
    AGENT_ERROR(20002, "Agent 执行异常"),
    AGENT_STEP_LIMIT(20003, "Agent 已达最大步骤限制"),
    AGENT_EMPTY_PROMPT(20004, "Agent 输入为空"),

    // ========== 工具调用 30xxx ==========
    TOOL_NOT_FOUND(30001, "工具不存在"),
    TOOL_EXECUTION_FAILED(30002, "工具执行失败"),
    TOOL_TIMEOUT(30003, "工具执行超时"),

    // ========== SSE / 流式 40xxx ==========
    SSE_CONNECTION_ERROR(40001, "SSE 连接异常"),
    SSE_SEND_FAILED(40002, "SSE 消息发送失败"),
    SSE_CLIENT_DISCONNECTED(40003, "客户端已断开"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", code, message);
    }
}
