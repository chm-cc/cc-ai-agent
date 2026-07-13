package com.chm.aiagent.agent.fallback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CapabilityDomain 枚举单元测试
 */
@DisplayName("CapabilityDomain 单元测试")
class CapabilityDomainTest {

    // ======================== findByTool ========================

    @Nested
    @DisplayName("findByTool — 工具→域查找")
    class FindByTool {

        @Test
        @DisplayName("queryWeather → INFORMATION_RETRIEVAL")
        void shouldMapWeatherToInfoRetrieval() {
            assertThat(CapabilityDomain.findByTool("queryWeather"))
                    .isEqualTo(CapabilityDomain.INFORMATION_RETRIEVAL);
        }

        @Test
        @DisplayName("searchWeb → INFORMATION_RETRIEVAL")
        void shouldMapSearchWebToInfoRetrieval() {
            assertThat(CapabilityDomain.findByTool("searchWeb"))
                    .isEqualTo(CapabilityDomain.INFORMATION_RETRIEVAL);
        }

        @Test
        @DisplayName("scrapeWebPage → INFORMATION_RETRIEVAL")
        void shouldMapScrapeToInfoRetrieval() {
            assertThat(CapabilityDomain.findByTool("scrapeWebPage"))
                    .isEqualTo(CapabilityDomain.INFORMATION_RETRIEVAL);
        }

        @Test
        @DisplayName("readFile → FILE_OPERATION")
        void shouldMapReadFileToFileOp() {
            assertThat(CapabilityDomain.findByTool("readFile"))
                    .isEqualTo(CapabilityDomain.FILE_OPERATION);
        }

        @Test
        @DisplayName("writeFile → FILE_OPERATION")
        void shouldMapWriteFileToFileOp() {
            assertThat(CapabilityDomain.findByTool("writeFile"))
                    .isEqualTo(CapabilityDomain.FILE_OPERATION);
        }

        @Test
        @DisplayName("executeTerminalCommand → TERMINAL_EXECUTION")
        void shouldMapTerminalToTerminalExec() {
            assertThat(CapabilityDomain.findByTool("executeTerminalCommand"))
                    .isEqualTo(CapabilityDomain.TERMINAL_EXECUTION);
        }

        @Test
        @DisplayName("doTerminate → FLOW_CONTROL")
        void shouldMapTerminateToFlowControl() {
            assertThat(CapabilityDomain.findByTool("doTerminate"))
                    .isEqualTo(CapabilityDomain.FLOW_CONTROL);
        }

        @Test
        @DisplayName("未知工具名 → null")
        void shouldReturnNullForUnknownTool() {
            assertThat(CapabilityDomain.findByTool("nonExistentTool")).isNull();
        }
    }

    // ======================== priorityOf ========================

    @Nested
    @DisplayName("priorityOf — 域内优先级")
    class PriorityOf {

        @Test
        @DisplayName("queryWeather 是信息获取域第一优先 → 1")
        void weatherIsFirstPriority() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.priorityOf("queryWeather")).isEqualTo(1);
        }

        @Test
        @DisplayName("searchWeb 是第二优先 → 2")
        void searchIsSecondPriority() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.priorityOf("searchWeb")).isEqualTo(2);
        }

        @Test
        @DisplayName("scrapeWebPage 是第三优先 → 3")
        void scrapeIsThirdPriority() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.priorityOf("scrapeWebPage")).isEqualTo(3);
        }

        @Test
        @DisplayName("不在域内的工具 → Integer.MAX_VALUE")
        void unknownToolIsMaxPriority() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.priorityOf("doTerminate"))
                    .isEqualTo(Integer.MAX_VALUE);
        }
    }

    // ======================== fallbackTools ========================

    @Nested
    @DisplayName("fallbackTools — 备用工具链")
    class FallbackTools {

        @Test
        @DisplayName("queryWeather 失败 → 备用 [searchWeb, scrapeWebPage]")
        void weatherFallbackIsSearchThenScrape() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.fallbackTools("queryWeather"))
                    .containsExactly("searchWeb", "scrapeWebPage");
        }

        @Test
        @DisplayName("searchWeb 失败 → 备用 [scrapeWebPage]")
        void searchFallbackIsScrape() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.fallbackTools("searchWeb"))
                    .containsExactly("scrapeWebPage");
        }

        @Test
        @DisplayName("scrapeWebPage 失败 → 无备用（域内最后一道）")
        void scrapeHasNoFallback() {
            assertThat(CapabilityDomain.INFORMATION_RETRIEVAL.fallbackTools("scrapeWebPage"))
                    .isEmpty();
        }

        @Test
        @DisplayName("readFile → 备用 [writeFile]")
        void readFileFallbackIsWrite() {
            assertThat(CapabilityDomain.FILE_OPERATION.fallbackTools("readFile"))
                    .containsExactly("writeFile");
        }

        @Test
        @DisplayName("executeTerminalCommand → 无备用（域内仅一个工具）")
        void terminalHasNoFallback() {
            assertThat(CapabilityDomain.TERMINAL_EXECUTION.fallbackTools("executeTerminalCommand"))
                    .isEmpty();
        }
    }

    // ======================== 结构校验 ========================

    @Test
    @DisplayName("共 5 个能力域，总计 9 个工具")
    void shouldHaveCorrectTotalCounts() {
        assertThat(CapabilityDomain.values()).hasSize(5);
        int totalTools = java.util.Arrays.stream(CapabilityDomain.values())
                .mapToInt(d -> d.getTools().size())
                .sum();
        assertThat(totalTools).isEqualTo(9);
    }

    @Test
    @DisplayName("所有域的 displayName 不为空")
    void allDomainsShouldHaveDisplayName() {
        for (CapabilityDomain domain : CapabilityDomain.values()) {
            assertThat(domain.getDisplayName()).isNotBlank();
        }
    }

    @Test
    @DisplayName("所有工具名唯一，无重复注册")
    void allToolNamesShouldBeUnique() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CapabilityDomain domain : CapabilityDomain.values()) {
            for (String tool : domain.getTools()) {
                assertThat(seen.add(tool))
                        .as("工具 " + tool + " 在多个域中重复注册")
                        .isTrue();
            }
        }
    }
}
