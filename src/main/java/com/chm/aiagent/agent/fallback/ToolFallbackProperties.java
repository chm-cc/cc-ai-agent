package com.chm.aiagent.agent.fallback;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具调用失败兜底配置，绑定 application.yaml 中 agent.tool-fallback.* 配置段
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.tool-fallback")
public class ToolFallbackProperties {

    /** LLM 调用重试配置 */
    private LlmRetry llm = new LlmRetry();

    /** 工具执行重试配置 */
    private ToolRetry tool = new ToolRetry();

    /** 降级控制 */
    private Degradation degradation = new Degradation();

    /** 熔断配置 */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class LlmRetry {
        /** 最大重试次数 */
        private int maxRetries = 3;
        /** 退避间隔（毫秒），数组长度应 >= maxRetries */
        private List<Long> backoffMs = List.of(1000L, 2000L, 4000L);
        /** 可重试的异常类名 */
        private List<String> retryableExceptions = List.of(
                "java.io.IOException",
                "java.net.SocketTimeoutException",
                "java.util.concurrent.TimeoutException"
        );
    }

    @Data
    public static class ToolRetry {
        /** 同工具最多重试次数 */
        private int maxRetries = 2;
        /** 退避倍数 */
        private int backoffMultiplier = 2;
        /** 单次工具调用超时（秒） */
        private int timeoutSeconds = 15;
    }

    @Data
    public static class Degradation {
        /** 最多降级轮次 */
        private int maxRounds = 2;
        /** 是否仅限同能力域内降级 */
        private boolean sameDomainOnly = true;
    }

    @Data
    public static class CircuitBreaker {
        /** 连续失败多少次触发熔断 */
        private int failureThreshold = 3;
        /** 熔断范围：conversation（单次对话） */
        private String scope = "conversation";
    }
}
