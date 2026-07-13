package com.chm.aiagent.agent.fallback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolFailureContext 错误分类与可重试判断的单元测试
 */
@DisplayName("ToolFailureContext 单元测试")
class ToolFailureContextTest {

    // ======================== classifyError ========================

    @Nested
    @DisplayName("classifyError — 错误码分类")
    class ClassifyError {

        @Test
        @DisplayName("null 输入 → TOOL_NULL")
        void shouldReturnToolNullForNullInput() {
            assertThat(ToolFailureContext.classifyError(null)).isEqualTo("TOOL_NULL");
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                Connection timed out                   | NET_TIMEOUT
                Request timed out after 5000ms         | NET_TIMEOUT
                connection refused                     | NET_TIMEOUT
                Connection reset by peer               | NET_TIMEOUT
                """)
        @DisplayName("超时/连接错误 → NET_TIMEOUT")
        void shouldClassifyTimeoutErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                Host unreachable                       | NET_UNREACHABLE
                UnknownHostException: api.example.com  | NET_UNREACHABLE
                DNS resolution failed                  | NET_UNREACHABLE
                """)
        @DisplayName("不可达/DNS错误 → NET_UNREACHABLE")
        void shouldClassifyUnreachableErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                HTTP 502 Bad Gateway                   | SVR_502
                Server returned 503 Service Unavailable| SVR_503
                Gateway timeout 504                    | SVR_504
                Internal Server Error 500              | SVR_500
                """)
        @DisplayName("服务端错误 → SVR_xxx")
        void shouldClassifyServerErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                参数无效                               | ARG_INVALID
                invalid argument                       | ARG_INVALID
                illegal parameter                      | ARG_INVALID
                格式错误                               | ARG_INVALID
                """)
        @DisplayName("参数错误 → ARG_INVALID")
        void shouldClassifyArgErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                API key exhausted                      | AUTH_KEY_EXHAUSTED
                Unauthorized                           | AUTH_KEY_EXHAUSTED
                forbidden access                       | AUTH_KEY_EXHAUSTED
                quota exceeded                         | AUTH_KEY_EXHAUSTED
                """)
        @DisplayName("权限/配额 → AUTH_KEY_EXHAUSTED")
        void shouldClassifyAuthErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                File not found                         | NF_NOT_FOUND
                resource does not exist                | NF_NOT_FOUND
                文件不存在                             | NF_NOT_FOUND
                未找到                                 | NF_NOT_FOUND
                no such file or directory              | NF_NOT_FOUND
                """)
        @DisplayName("资源不存在 → NF_NOT_FOUND")
        void shouldClassifyNotFoundErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                permission denied                      | AUTH_DENIED
                权限不足                               | AUTH_DENIED
                """)
        @DisplayName("权限拒绝 → AUTH_DENIED")
        void shouldClassifyPermissionErrors(String message, String expectedCode) {
            assertThat(ToolFailureContext.classifyError(message)).isEqualTo(expectedCode);
        }

        @Test
        @DisplayName("未识别的错误 → TOOL_ERROR")
        void shouldReturnToolErrorForUnknown() {
            assertThat(ToolFailureContext.classifyError("some random error message"))
                    .isEqualTo("TOOL_ERROR");
        }

        @Test
        @DisplayName("空字符串 → TOOL_ERROR（不匹配任何已知模式）")
        void shouldReturnToolErrorForEmptyString() {
            assertThat(ToolFailureContext.classifyError("")).isEqualTo("TOOL_ERROR");
        }

        @Test
        @DisplayName("英文大小写不敏感")
        void shouldBeCaseInsensitive() {
            assertThat(ToolFailureContext.classifyError("TIMEOUT occurred"))
                    .isEqualTo("NET_TIMEOUT");
            assertThat(ToolFailureContext.classifyError("INTERNAL SERVER ERROR"))
                    .isEqualTo("SVR_500");
        }
    }

    // ======================== isRetryable ========================

    @Nested
    @DisplayName("isRetryable — 可重试判断")
    class IsRetryable {

        @Test
        @DisplayName("null → false")
        void shouldReturnFalseForNull() {
            assertThat(ToolFailureContext.isRetryable(null)).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
                "NET_TIMEOUT, true",
                "NET_UNREACHABLE, true",
                "SVR_502, true",
                "SVR_503, true",
                "SVR_500, true",
                "SVR_504, true",
        })
        @DisplayName("NET_ / SVR_5xx → 可重试")
        void shouldBeRetryable(String code, boolean expected) {
            assertThat(ToolFailureContext.isRetryable(code)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "ARG_INVALID, false",
                "AUTH_KEY_EXHAUSTED, false",
                "NF_NOT_FOUND, false",
                "AUTH_DENIED, false",
                "TOOL_ERROR, false",
                "TOOL_NULL, false",
        })
        @DisplayName("参数/权限/不存在错误 → 不可重试")
        void shouldNotBeRetryable(String code, boolean expected) {
            assertThat(ToolFailureContext.isRetryable(code)).isEqualTo(expected);
        }
    }

    // ======================== Builder ========================

    @Test
    @DisplayName("Builder 正确构建所有字段")
    void shouldBuildCorrectly() {
        ToolFailureContext ctx = ToolFailureContext.builder()
                .toolName("queryWeather")
                .toolArgs("{\"city\":\"北京\"}")
                .errorMessage("Connection timed out")
                .errorCode("NET_TIMEOUT")
                .retryable(true)
                .failureCount(2)
                .circuitBroken(false)
                .degradationRound(1)
                .maxDegradationRounds(2)
                .fallbackTools(java.util.List.of("searchWeb", "scrapeWebPage"))
                .build();

        assertThat(ctx.getToolName()).isEqualTo("queryWeather");
        assertThat(ctx.getToolArgs()).isEqualTo("{\"city\":\"北京\"}");
        assertThat(ctx.getErrorCode()).isEqualTo("NET_TIMEOUT");
        assertThat(ctx.isRetryable()).isTrue();
        assertThat(ctx.getFailureCount()).isEqualTo(2);
        assertThat(ctx.isCircuitBroken()).isFalse();
        assertThat(ctx.getFallbackTools()).containsExactly("searchWeb", "scrapeWebPage");
    }
}
