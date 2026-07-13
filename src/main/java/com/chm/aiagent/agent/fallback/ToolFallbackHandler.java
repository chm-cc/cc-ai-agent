package com.chm.aiagent.agent.fallback;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具调用失败兜底处理器 —— 五层兜底架构的核心编排逻辑
 *
 * <pre>
 * Layer 0: 失败检测 + 错误码分类
 * Layer 1: 同工具指数退避重试（由 ToolCallAgent 控制）
 * Layer 2: 能力域内降级（推荐同域备用工具给 LLM）
 * Layer 3: LLM 自主决策（自由选择工具或告知用户）
 * Layer 4: 优雅降级告知（诚实告知用户失败原因）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolFallbackHandler {

    private final ToolCapabilityRegistry capabilityRegistry;
    private final ToolFallbackProperties properties;

    /** 常见的工具失败标记模式 */
    private static final List<String> FAILURE_PATTERNS = List.of(
            "Error", "error", "失败", "failed", "异常", "exception",
            "timeout", "timed out", "unreachable", "refused",
            "不能", "无法", "cannot", "unable"
    );

    /**
     * 检测 ToolResponseMessage 中各工具的执行结果，返回失败列表
     */
    public List<ToolFailureContext> detectFailures(
            ToolResponseMessage toolResponseMessage,
            Map<String, String> toolArgsMap,  // toolName → args JSON
            int degradationRound,
            Set<String> circuitBrokenTools,
            Map<String, Integer> failureCounts) {

        List<ToolFailureContext> failures = new ArrayList<>();

        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            String toolName = response.name();
            String result = response.responseData();

            if (isFailure(result)) {
                String errorCode = ToolFailureContext.classifyError(result);
                boolean retryable = ToolFailureContext.isRetryable(errorCode);
                int failureCount = failureCounts.getOrDefault(toolName, 0) + 1;
                boolean circuitBroken = circuitBrokenTools.contains(toolName);

                List<String> fallbackTools = new ArrayList<>();
                if (properties.getDegradation().isSameDomainOnly()) {
                    fallbackTools = capabilityRegistry.getFallbackTools(toolName);
                }
                // 如果不是仅限同域，则收集所有域中不是当前工具的其他工具
                if (!properties.getDegradation().isSameDomainOnly() && fallbackTools.isEmpty()) {
                    CapabilityDomain currentDomain = capabilityRegistry.getDomain(toolName);
                    fallbackTools = Arrays.stream(CapabilityDomain.values())
                            .filter(d -> d != currentDomain)
                            .flatMap(d -> d.getTools().stream())
                            .filter(t -> !t.equals(toolName))
                            .collect(Collectors.toList());
                }

                // 过滤掉已熔断的工具
                fallbackTools = fallbackTools.stream()
                        .filter(t -> !circuitBrokenTools.contains(t))
                        .collect(Collectors.toList());

                ToolFailureContext ctx = ToolFailureContext.builder()
                        .toolName(toolName)
                        .toolArgs(toolArgsMap.getOrDefault(toolName, "{}"))
                        .errorMessage(result)
                        .errorCode(errorCode)
                        .retryable(retryable)
                        .failureCount(failureCount)
                        .circuitBroken(circuitBroken)
                        .degradationRound(degradationRound)
                        .maxDegradationRounds(properties.getDegradation().getMaxRounds())
                        .fallbackTools(fallbackTools)
                        .build();

                failures.add(ctx);
                log.warn("工具调用失败: tool={}, errorCode={}, retryable={}, failureCount={}",
                        toolName, errorCode, retryable, failureCount);
            }
        }

        return failures;
    }

    /**
     * 构建注入 LLM 对话的失败反馈消息
     */
    public String buildFallbackMessage(List<ToolFailureContext> failures, int remainingRounds) {
        StringBuilder sb = new StringBuilder();
        sb.append("【工具执行反馈】\n\n");

        for (int i = 0; i < failures.size(); i++) {
            ToolFailureContext f = failures.get(i);
            if (i > 0) sb.append("---\n\n");

            sb.append("❌ ").append(f.getToolName())
                    .append("(").append(f.getToolArgs()).append(") 执行失败\n");
            sb.append("   失败原因：").append(truncate(f.getErrorMessage(), 200)).append("\n");
            sb.append("   错误类型：").append(f.getErrorCode());

            if (f.isRetryable()) {
                sb.append("（可重试）\n");
            } else {
                sb.append("（不建议重试）\n");
            }

            // 熔断提示
            if (f.isCircuitBroken()) {
                sb.append("\n⚠️ ").append(f.getToolName())
                        .append(" 已被熔断（累计失败 ").append(f.getFailureCount())
                        .append(" 次），本对话中请勿再调用此工具。\n");
            } else if (f.getFailureCount() >= properties.getCircuitBreaker().getFailureThreshold()) {
                sb.append("\n⚠️ ").append(f.getToolName())
                        .append(" 已达到熔断阈值（").append(f.getFailureCount())
                        .append(" 次失败），本对话中请勿再调用此工具。\n");
            }
        }

        // 备用方案
        sb.append("\n🔧 建议的备用方案：\n");

        Set<String> allFallbacks = new LinkedHashSet<>();
        for (ToolFailureContext f : failures) {
            allFallbacks.addAll(f.getFallbackTools());
        }

        char option = 'A';
        if (!allFallbacks.isEmpty()) {
            for (String tool : allFallbacks) {
                CapabilityDomain domain = capabilityRegistry.getDomain(tool);
                String domainName = domain != null ? domain.getDisplayName() : "其他";
                sb.append("   方案 ").append(option).append("：调用 ")
                        .append(tool).append("（").append(domainName).append("域）\n");
                option++;
            }
        }

        // 直接告知用户
        sb.append("   方案 ").append(option).append("：放弃使用工具，直接告知用户当前操作无法完成，")
                .append("并诚实说明原因和建议\n");

        // 剩余轮次
        sb.append("\n📋 当前状态：剩余降级轮次 ").append(remainingRounds);
        if (remainingRounds <= 0) {
            sb.append("（已耗尽！本轮必须选择方案 ").append(option).append("，用文本回复结束对话）");
        }
        sb.append("\n\n请选择最合适的方案并继续。");

        return sb.toString();
    }

    /**
     * 构建降级轮次耗尽后的最终消息，强制 LLM 以文本回复
     */
    public String buildFinalFallbackMessage(List<ToolFailureContext> failures) {
        String toolNames = failures.stream()
                .map(ToolFailureContext::getToolName)
                .distinct()
                .collect(Collectors.joining("、"));

        return String.format("""
                【工具执行反馈 - 最终轮】

                ❌ 以下工具均已尝试并失败：%s
                ⚠️ 降级轮次已耗尽，不可再调用任何工具。

                请用文本直接回复用户：
                1. 诚实告知哪些操作未能完成
                2. 说明失败原因（如：天气服务不可用、搜索 API 超时等）
                3. 给出用户可以自行解决的建议
                4. 主动询问用户是否还有其他需要帮助的事项

                请立即回复。""", toolNames);
    }

    /**
     * 判断工具返回内容是否表示失败
     */
    public boolean isFailure(String toolResponse) {
        if (StrUtil.isBlank(toolResponse)) return false;

        String lower = toolResponse.toLowerCase();
        for (String pattern : FAILURE_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断异常是否可重试（用于 LLM 调用层面）
     */
    public boolean isRetryableException(Throwable e) {
        if (e == null) return false;
        String className = e.getClass().getName();
        for (String retryable : properties.getLlm().getRetryableExceptions()) {
            if (className.equals(retryable) || className.contains(
                    retryable.substring(retryable.lastIndexOf('.') + 1))) {
                return true;
            }
        }
        // 兜底：检查 cause chain
        Throwable cause = e.getCause();
        while (cause != null) {
            if (isRetryableException(cause)) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "无";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
