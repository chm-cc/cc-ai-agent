package com.chm.aiagent.agent.fallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolCapabilityRegistry 单元测试
 */
@DisplayName("ToolCapabilityRegistry 单元测试")
class ToolCapabilityRegistryTest {

    private ToolCapabilityRegistry registry;

    @BeforeEach
    void setUp() {
        // ToolFallbackProperties 仅被构造函数引用但不使用，传任意非 null 即可
        ToolFallbackProperties props = new ToolFallbackProperties();
        registry = new ToolCapabilityRegistry(props);
    }

    @Nested
    @DisplayName("getDomain — 工具→域查找")
    class GetDomain {

        @Test
        @DisplayName("queryWeather → INFORMATION_RETRIEVAL")
        void weatherInInfoRetrieval() {
            assertThat(registry.getDomain("queryWeather"))
                    .isEqualTo(CapabilityDomain.INFORMATION_RETRIEVAL);
        }

        @Test
        @DisplayName("readFile → FILE_OPERATION")
        void readFileInFileOp() {
            assertThat(registry.getDomain("readFile"))
                    .isEqualTo(CapabilityDomain.FILE_OPERATION);
        }

        @Test
        @DisplayName("未知工具 → null")
        void unknownReturnsNull() {
            assertThat(registry.getDomain("ghostTool")).isNull();
        }
    }

    @Nested
    @DisplayName("getFallbackTools — 备用工具推荐")
    class GetFallbackTools {

        @Test
        @DisplayName("queryWeather → [searchWeb, scrapeWebPage]")
        void weatherFallbacks() {
            assertThat(registry.getFallbackTools("queryWeather"))
                    .containsExactly("searchWeb", "scrapeWebPage");
        }

        @Test
        @DisplayName("writeFile → []（是域内最后工具）")
        void writeFileNoFallback() {
            assertThat(registry.getFallbackTools("writeFile")).isEmpty();
        }

        @Test
        @DisplayName("未知工具 → []")
        void unknownNoFallback() {
            assertThat(registry.getFallbackTools("ghostTool")).isEmpty();
        }
    }

    @Nested
    @DisplayName("sameDomain — 同域判断")
    class SameDomain {

        @Test
        @DisplayName("queryWeather 与 searchWeb → true")
        void weatherAndSearchSameDomain() {
            assertThat(registry.sameDomain("queryWeather", "searchWeb")).isTrue();
        }

        @Test
        @DisplayName("queryWeather 与 readFile → false")
        void weatherAndFileDifferentDomain() {
            assertThat(registry.sameDomain("queryWeather", "readFile")).isFalse();
        }

        @Test
        @DisplayName("两个 null → false")
        void nullReturnsFalse() {
            assertThat(registry.sameDomain(null, null)).isFalse();
        }

        @Test
        @DisplayName("一个 null → false")
        void oneNullReturnsFalse() {
            assertThat(registry.sameDomain("queryWeather", null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority — 域内优先级")
    class GetPriority {

        @Test
        @DisplayName("queryWeather = 1（最高）")
        void weatherPriority1() {
            assertThat(registry.getPriority("queryWeather")).isEqualTo(1);
        }

        @Test
        @DisplayName("searchWeb = 2")
        void searchPriority2() {
            assertThat(registry.getPriority("searchWeb")).isEqualTo(2);
        }

        @Test
        @DisplayName("未知工具 = Integer.MAX_VALUE")
        void unknownMaxPriority() {
            assertThat(registry.getPriority("ghostTool")).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("getToolsInDomain — 域内完整工具列表")
    class GetToolsInDomain {

        @Test
        @DisplayName("INFORMATION_RETRIEVAL → 3 个工具")
        void infoRetrievalHas3Tools() {
            List<String> tools = registry.getToolsInDomain(CapabilityDomain.INFORMATION_RETRIEVAL);
            assertThat(tools).containsExactly("queryWeather", "searchWeb", "scrapeWebPage");
        }

        @Test
        @DisplayName("FLOW_CONTROL → 1 个工具")
        void flowControlHas1Tool() {
            assertThat(registry.getToolsInDomain(CapabilityDomain.FLOW_CONTROL))
                    .containsExactly("doTerminate");
        }
    }
}
