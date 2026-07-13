package com.chm.aiagent.agent.fallback;

import lombok.Builder;
import lombok.Data;

/**
 * 工具调用失败的上下文信息，用于在对话中注入结构化的失败反馈
 */
@Data
@Builder
public class ToolFailureContext {

    /** 工具名称 */
    private String toolName;

    /** 调用参数（JSON 字符串） */
    private String toolArgs;

    /** 工具返回的错误信息 */
    private String errorMessage;

    /** 错误码（NET_TIMEOUT / ARG_INVALID / SVR_502 ...） */
    private String errorCode;

    /** 是否可重试 */
    private boolean retryable;

    /** 该工具在本次对话中的累计失败次数 */
    private int failureCount;

    /** 是否已被熔断 */
    private boolean circuitBroken;

    /** 当前降级轮次 */
    private int degradationRound;

    /** 最大降级轮次 */
    private int maxDegradationRounds;

    /** 同域内可用的备用工具列表 */
    private java.util.List<String> fallbackTools;

    /**
     * 错误码分类：根据工具返回内容自动判断
     */
    public static String classifyError(String toolResponse) {
        if (toolResponse == null) return "TOOL_NULL";

        String lower = toolResponse.toLowerCase();

        // 网络/超时
        if (lower.contains("timeout") || lower.contains("timed out")
                || lower.contains("connect") && (lower.contains("refused") || lower.contains("reset"))) {
            return "NET_TIMEOUT";
        }
        if (lower.contains("unreachable") || lower.contains("host")
                || lower.contains("dns") || lower.contains("unknownhost")) {
            return "NET_UNREACHABLE";
        }

        // 服务端错误
        if (lower.contains("502") || lower.contains("503") || lower.contains("504")) {
            return "SVR_" + (lower.contains("502") ? "502" : lower.contains("503") ? "503" : "504");
        }
        if (lower.contains("500") || lower.contains("internal server error")) {
            return "SVR_500";
        }

        // 参数错误
        if (lower.contains("参数") || lower.contains("invalid")
                || lower.contains("illegal") || lower.contains("格式")) {
            return "ARG_INVALID";
        }

        // 权限/配额
        if (lower.contains("key") || lower.contains("auth") || lower.contains("unauthorized")
                || lower.contains("forbidden") || lower.contains("quota") || lower.contains("exhausted")) {
            return "AUTH_KEY_EXHAUSTED";
        }

        // 资源不存在
        if (lower.contains("not found") || lower.contains("not exist") || lower.contains("no such")
                || lower.contains("不存在") || lower.contains("未找到")) {
            return "NF_NOT_FOUND";
        }

        // 权限（文件/命令）
        if (lower.contains("permission") || lower.contains("denied") || lower.contains("权限")) {
            return "AUTH_DENIED";
        }

        return "TOOL_ERROR";
    }

    /**
     * 判断该错误码对应的错误是否可重试
     */
    public static boolean isRetryable(String errorCode) {
        if (errorCode == null) return false;
        return errorCode.startsWith("NET_") || errorCode.startsWith("SVR_5");
    }
}
