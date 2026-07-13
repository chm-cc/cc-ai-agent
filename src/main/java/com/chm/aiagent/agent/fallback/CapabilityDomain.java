package com.chm.aiagent.agent.fallback;

import java.util.List;
import java.util.Map;

/**
 * 工具能力域枚举，定义工具所属的能力域及域内备用工具链
 */
public enum CapabilityDomain {

    /** 信息获取：天气查询 → 搜索 → 网页抓取，按精确度降级 */
    INFORMATION_RETRIEVAL("信息获取", List.of("queryWeather", "searchWeb", "scrapeWebPage")),

    /** 文件操作：读写文件 */
    FILE_OPERATION("文件操作", List.of("readFile", "writeFile")),

    /** 资源处理：下载 → PDF 生成 */
    RESOURCE_PROCESSING("资源处理", List.of("downloadResource", "generatePDF")),

    /** 终端执行 */
    TERMINAL_EXECUTION("终端执行", List.of("executeTerminalCommand")),

    /** 流程控制 */
    FLOW_CONTROL("流程控制", List.of("doTerminate"));

    private final String displayName;
    private final List<String> tools;

    CapabilityDomain(String displayName, List<String> tools) {
        this.displayName = displayName;
        this.tools = tools;
    }

    public String getDisplayName() { return displayName; }

    /** 域内工具按优先级排序，index 0 是首选 */
    public List<String> getTools() { return tools; }

    /** 根据工具名查找其所属的能力域 */
    public static CapabilityDomain findByTool(String toolName) {
        for (CapabilityDomain domain : values()) {
            if (domain.tools.contains(toolName)) {
                return domain;
            }
        }
        return null;
    }

    /** 获取工具在域内的优先级（越小越优先，1-based） */
    public int priorityOf(String toolName) {
        int idx = tools.indexOf(toolName);
        return idx >= 0 ? idx + 1 : Integer.MAX_VALUE;
    }

    /** 获取域内比当前工具低优先级的备用工具列表 */
    public List<String> fallbackTools(String toolName) {
        int idx = tools.indexOf(toolName);
        if (idx < 0) return List.of();
        return tools.subList(idx + 1, tools.size());
    }
}
