package com.chm.aiagent.agent.fallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolFallbackHandler 单元测试
 */
@DisplayName("ToolFallbackHandler 单元测试")
class ToolFallbackHandlerTest {

    private ToolFallbackHandler handler;
    private ToolCapabilityRegistry registry;
    private ToolFallbackProperties properties;
    private Map<String, String> toolArgsMap;
    private Set<String> circuitBrokenTools;
    private Map<String, Integer> failureCounts;

    @BeforeEach
    void setUp() {
        properties = new ToolFallbackProperties();
        registry = new ToolCapabilityRegistry(properties);
        handler = new ToolFallbackHandler(registry, properties);

        toolArgsMap = new LinkedHashMap<>();
        toolArgsMap.put("queryWeather", "{\"city\":\"北京\"}");
        toolArgsMap.put("searchWeb", "{\"query\":\"北京天气\"}");

        circuitBrokenTools = new LinkedHashSet<>();
        failureCounts = new LinkedHashMap<>();
    }

    // ======================== isFailure ========================

    @Nested
    @DisplayName("isFailure — 失败检测")
    class IsFailure {

        @Test
        @DisplayName("包含 Error → true")
        void errorMarkedAsFailure() {
            assertThat(handler.isFailure("Error connecting to server")).isTrue();
        }

        @Test
        @DisplayName("包含 失败 → true")
        void chineseFailedAsFailure() {
            assertThat(handler.isFailure("查询天气失败")).isTrue();
        }

        @Test
        @DisplayName("包含 timeout → true")
        void timeoutAsFailure() {
            assertThat(handler.isFailure("Connection timeout after 5000ms")).isTrue();
        }

        @Test
        @DisplayName("包含 exception → true")
        void exceptionAsFailure() {
            assertThat(handler.isFailure("java.lang.Exception: something wrong")).isTrue();
        }

        @Test
        @DisplayName("正常天气数据 → false")
        void normalDataNotFailure() {
            String normal = "北京当前天气：晴，温度 25°C，湿度 45%";
            assertThat(handler.isFailure(normal)).isFalse();
        }

        @Test
        @DisplayName("null → false")
        void nullNotFailure() {
            assertThat(handler.isFailure(null)).isFalse();
        }

        @Test
        @DisplayName("空字符串 → false")
        void emptyNotFailure() {
            assertThat(handler.isFailure("")).isFalse();
        }
    }

    // ======================== detectFailures ========================

    @Nested
    @DisplayName("detectFailures — 批量失败检测")
    class DetectFailures {

        @Test
        @DisplayName("全部成功 → 空列表")
        void noFailuresReturnsEmpty() {
            ToolResponseMessage msg = buildToolResponse(
                    Map.of(
                            "queryWeather", "北京天气：晴，25°C",
                            "searchWeb", "搜索结果：今日北京晴朗"
                    )
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);
            assertThat(failures).isEmpty();
        }

        @Test
        @DisplayName("一个失败 → 返回 1 个失败上下文")
        void singleFailureDetected() {
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("queryWeather", "Connection timeout", "searchWeb", "OK")
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);

            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).getToolName()).isEqualTo("queryWeather");
            assertThat(failures.get(0).getErrorCode()).isEqualTo("NET_TIMEOUT");
            assertThat(failures.get(0).isRetryable()).isTrue();
        }

        @Test
        @DisplayName("失败工具有 args → 上下文中包含 args")
        void failureContextContainsArgs() {
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("queryWeather", "Host unreachable")
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);

            assertThat(failures.get(0).getToolArgs()).isEqualTo("{\"city\":\"北京\"}");
        }

        @Test
        @DisplayName("失败次数正确累积")
        void failureCountAccumulated() {
            failureCounts.put("queryWeather", 2); // 之前已失败 2 次
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("queryWeather", "timeout again")
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);

            // detectFailures 从 failureCounts map 中读到 2，再 +1 = 3
            assertThat(failures.get(0).getFailureCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("已熔断工具被标记 circuitBroken=true")
        void circuitBrokenMarked() {
            circuitBrokenTools.add("queryWeather");
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("queryWeather", "Error: still failing")
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);

            assertThat(failures.get(0).isCircuitBroken()).isTrue();
        }

        @Test
        @DisplayName("备用工具列表过滤掉已熔断的工具")
        void fallbackExcludesCircuitBroken() {
            // queryWeather 失败 → 备用是 searchWeb + scrapeWebPage
            // 但 searchWeb 已熔断 → 只剩 scrapeWebPage
            circuitBrokenTools.add("searchWeb");
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("queryWeather", "timeout")
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);

            assertThat(failures.get(0).getFallbackTools())
                    .containsExactly("scrapeWebPage");
        }

        @Test
        @DisplayName("非仅限同域时，跨域提供备用工具")
        void crossDomainFallback() {
            properties.getDegradation().setSameDomainOnly(false);
            // executeTerminalCommand 在 TERMINAL_EXECUTION 域，该域只有自己
            // sameDomainOnly=false → 从其他域收集备用
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("executeTerminalCommand", "Error: permission denied")
            );
            Map<String, String> terminalArgs = Map.of("executeTerminalCommand", "{\"cmd\":\"ls\"}");
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, terminalArgs, 0, circuitBrokenTools, failureCounts);

            assertThat(failures.get(0).getFallbackTools()).isNotEmpty();
        }

        @Test
        @DisplayName("参数不可重试的错误码 → retryable=false")
        void nonRetryableError() {
            ToolResponseMessage msg = buildToolResponse(
                    Map.of("queryWeather", "Error: API key exhausted")
            );
            List<ToolFailureContext> failures = handler.detectFailures(
                    msg, toolArgsMap, 0, circuitBrokenTools, failureCounts);

            assertThat(failures.get(0).getErrorCode()).isEqualTo("AUTH_KEY_EXHAUSTED");
            assertThat(failures.get(0).isRetryable()).isFalse();
        }
    }

    // ======================== buildFallbackMessage ========================

    @Nested
    @DisplayName("buildFallbackMessage — 降级消息构建")
    class BuildFallbackMessage {

        @Test
        @DisplayName("包含工具名、错误类型、备用方案")
        void containsToolErrorAndAlternatives() {
            ToolFailureContext ctx = ToolFailureContext.builder()
                    .toolName("queryWeather")
                    .toolArgs("{\"city\":\"北京\"}")
                    .errorMessage("Connection timeout")
                    .errorCode("NET_TIMEOUT")
                    .retryable(true)
                    .failureCount(1)
                    .circuitBroken(false)
                    .degradationRound(1)
                    .maxDegradationRounds(2)
                    .fallbackTools(List.of("searchWeb", "scrapeWebPage"))
                    .build();

            String msg = handler.buildFallbackMessage(List.of(ctx), 1);

            assertThat(msg).contains("queryWeather");
            assertThat(msg).contains("NET_TIMEOUT");
            assertThat(msg).contains("searchWeb");
            assertThat(msg).contains("scrapeWebPage");
            assertThat(msg).contains("方案 A");
            assertThat(msg).contains("方案 B");
            // 方案 C = 放弃工具直接用文本
            assertThat(msg).contains("放弃使用工具");
        }

        @Test
        @DisplayName("剩余 0 轮 → 提示已耗尽")
        void zeroRemainingRoundsWarning() {
            ToolFailureContext ctx = ToolFailureContext.builder()
                    .toolName("queryWeather")
                    .toolArgs("{}")
                    .errorMessage("timeout")
                    .errorCode("NET_TIMEOUT")
                    .retryable(true)
                    .failureCount(1)
                    .circuitBroken(false)
                    .degradationRound(2)
                    .maxDegradationRounds(2)
                    .fallbackTools(List.of("searchWeb"))
                    .build();

            String msg = handler.buildFallbackMessage(List.of(ctx), 0);

            assertThat(msg).contains("已耗尽");
        }

        @Test
        @DisplayName("熔断工具提示")
        void circuitBrokenWarning() {
            ToolFailureContext ctx = ToolFailureContext.builder()
                    .toolName("queryWeather")
                    .toolArgs("{}")
                    .errorMessage("timeout")
                    .errorCode("NET_TIMEOUT")
                    .retryable(true)
                    .failureCount(3)
                    .circuitBroken(true)
                    .degradationRound(1)
                    .maxDegradationRounds(2)
                    .fallbackTools(List.of())
                    .build();

            String msg = handler.buildFallbackMessage(List.of(ctx), 1);

            assertThat(msg).contains("已被熔断");
            assertThat(msg).contains("请勿再调用");
        }
    }

    // ======================== buildFinalFallbackMessage ========================

    @Nested
    @DisplayName("buildFinalFallbackMessage — 最终兜底消息")
    class BuildFinalFallbackMessage {

        @Test
        @DisplayName("包含所有失败工具名和文本引导")
        void containsAllFailedTools() {
            ToolFailureContext ctx1 = ToolFailureContext.builder()
                    .toolName("queryWeather").toolArgs("{}")
                    .errorMessage("timeout").errorCode("NET_TIMEOUT")
                    .retryable(true).failureCount(1).circuitBroken(false)
                    .degradationRound(2).maxDegradationRounds(2)
                    .fallbackTools(List.of())
                    .build();
            ToolFailureContext ctx2 = ToolFailureContext.builder()
                    .toolName("searchWeb").toolArgs("{}")
                    .errorMessage("API key exhausted").errorCode("AUTH_KEY_EXHAUSTED")
                    .retryable(false).failureCount(1).circuitBroken(false)
                    .degradationRound(2).maxDegradationRounds(2)
                    .fallbackTools(List.of())
                    .build();

            String msg = handler.buildFinalFallbackMessage(List.of(ctx1, ctx2));

            assertThat(msg).contains("queryWeather");
            assertThat(msg).contains("searchWeb");
            assertThat(msg).contains("降级轮次已耗尽");
            assertThat(msg).contains("诚实告知");
            assertThat(msg).contains("不可再调用任何工具");
        }
    }

    // ======================== isRetryableException ========================

    @Nested
    @DisplayName("isRetryableException — 异常可重试判断")
    class IsRetryableException {

        @Test
        @DisplayName("IOException → true")
        void ioExceptionRetryable() {
            assertThat(handler.isRetryableException(new java.io.IOException("timeout"))).isTrue();
        }

        @Test
        @DisplayName("SocketTimeoutException → true")
        void socketTimeoutRetryable() {
            assertThat(handler.isRetryableException(
                    new java.net.SocketTimeoutException("read timed out"))).isTrue();
        }

        @Test
        @DisplayName("IllegalArgumentException → false")
        void illegalArgNotRetryable() {
            assertThat(handler.isRetryableException(new IllegalArgumentException("bad arg"))).isFalse();
        }

        @Test
        @DisplayName("null → false")
        void nullNotRetryable() {
            assertThat(handler.isRetryableException(null)).isFalse();
        }

        @Test
        @DisplayName("包装的 IOException cause → true")
        void wrappedIOExceptionRetryable() {
            RuntimeException wrapped = new RuntimeException(
                    new java.io.IOException("connection reset"));
            assertThat(handler.isRetryableException(wrapped)).isTrue();
        }
    }

    // ======================== 辅助方法 ========================

    private ToolResponseMessage buildToolResponse(Map<String, String> results) {
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            responses.add(new ToolResponseMessage.ToolResponse(
                    UUID.randomUUID().toString(), entry.getKey(), entry.getValue()));
        }
        return new ToolResponseMessage(responses, java.util.Collections.emptyMap());
    }
}
