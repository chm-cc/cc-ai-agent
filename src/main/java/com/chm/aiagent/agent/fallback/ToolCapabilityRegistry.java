package com.chm.aiagent.agent.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具能力域注册表 —— 维护工具与能力域的映射关系，提供 fallback 工具推荐
 */
@Slf4j
@Component
public class ToolCapabilityRegistry {

    private final ToolFallbackProperties properties;

    /** toolName → CapabilityDomain */
    private final Map<String, CapabilityDomain> toolDomainMap = new LinkedHashMap<>();

    /** CapabilityDomain → 域内按优先级排序的工具列表 */
    private final Map<CapabilityDomain, List<String>> domainTools = new LinkedHashMap<>();

    public ToolCapabilityRegistry(ToolFallbackProperties properties) {
        this.properties = properties;
        buildRegistry();
    }

    /** 从 CapabilityDomain 枚举构建工具-域映射 */
    private void buildRegistry() {
        for (CapabilityDomain domain : CapabilityDomain.values()) {
            List<String> tools = new ArrayList<>(domain.getTools());
            domainTools.put(domain, tools);
            for (String tool : tools) {
                toolDomainMap.putIfAbsent(tool, domain);
            }
        }

        log.info("工具能力域注册表初始化完成，共 {} 个域，{} 个工具", domainTools.size(), toolDomainMap.size());
        domainTools.forEach((domain, tools) ->
                log.info("  [{}] → {}", domain.getDisplayName(), String.join(" > ", tools)));
    }

    /** 查找工具所属的能力域 */
    public CapabilityDomain getDomain(String toolName) {
        return toolDomainMap.get(toolName);
    }

    /** 获取同一个能力域内的备用工具（按优先级排序） */
    public List<String> getFallbackTools(String toolName) {
        CapabilityDomain domain = toolDomainMap.get(toolName);
        if (domain == null) return List.of();
        return domain.fallbackTools(toolName);
    }

    /** 获取指定能力域内的所有工具 */
    public List<String> getToolsInDomain(CapabilityDomain domain) {
        return domainTools.getOrDefault(domain, List.of());
    }

    /** 判断两个工具是否属于同一能力域 */
    public boolean sameDomain(String toolA, String toolB) {
        CapabilityDomain domainA = toolDomainMap.get(toolA);
        CapabilityDomain domainB = toolDomainMap.get(toolB);
        return domainA != null && domainA == domainB;
    }

    /** 获取工具在能力域内的优先级（1 最高） */
    public int getPriority(String toolName) {
        CapabilityDomain domain = toolDomainMap.get(toolName);
        if (domain == null) return Integer.MAX_VALUE;
        return domain.priorityOf(toolName);
    }
}
