package com.chm.aiagent.service;

import com.chm.aiagent.config.AgentConfig;
import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.model.Agent;
import com.chm.aiagent.repository.AgentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentConfig agentConfig;
    private final ObjectMapper objectMapper;

    // ========== Seed: 首次启动从 YAML 导入种子数据 ==========

    @PostConstruct
    public void seedFromYaml() {
        if (agentRepository.count() > 0) {
            log.info("Agents table already populated, skipping seed");
            return;
        }

        List<AgentVO> yamlAgents = agentConfig.getAgents();
        if (yamlAgents == null || yamlAgents.isEmpty()) {
            log.warn("No agents defined in YAML, skipping seed");
            return;
        }

        log.info("Seeding {} agents from application.yaml", yamlAgents.size());
        for (AgentVO vo : yamlAgents) {
            Agent a = new Agent();
            a.setId(vo.getId());
            a.setName(vo.getName());
            a.setDescription(vo.getDescription());
            a.setIcon(vo.getIcon());
            a.setCategory(vo.getCategory() != null ? vo.getCategory() : "general");
            a.setTags(toJson(vo.getTags()));
            a.setStatus(vo.getStatus() != null ? vo.getStatus() : "ACTIVE");
            a.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : 0);
            a.setModel("");
            a.setTemperature(0.7);
            a.setMaxTokens(2000);

            // 为已知 Agent 设置默认 System Prompt
            if ("car-advisor".equals(a.getId())) {
                a.setSystemPrompt(CAR_ADVISOR_DEFAULT_PROMPT);
            } else if ("super-agent".equals(a.getId())) {
                a.setSystemPrompt(SUPER_AGENT_DEFAULT_PROMPT);
            } else {
                a.setSystemPrompt("");
            }
            a.setTools("[]");
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            agentRepository.insert(a);
        }
    }

    // ========== CRUD ==========

    public List<AgentVO> listActive() {
        return agentRepository.findActive().stream().map(this::toVO).toList();
    }

    public List<AgentVO> listAll() {
        return agentRepository.findAll().stream().map(this::toVO).toList();
    }

    public AgentVO getById(String agentId) {
        Agent a = agentRepository.findById(agentId);
        return a != null ? toVO(a) : null;
    }

    public AgentVO create(AgentVO vo) {
        Agent a = new Agent();
        a.setId(vo.getId());
        applyVO(a, vo);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        agentRepository.insert(a);
        return toVO(a);
    }

    public AgentVO update(String id, AgentVO vo) {
        Agent a = agentRepository.findById(id);
        if (a == null) return null;
        applyVO(a, vo);
        a.setUpdatedAt(LocalDateTime.now());
        agentRepository.update(a);
        return toVO(a);
    }

    public boolean delete(String id) {
        // 保护：不允许删除系统保留 Agent
        if ("car-advisor".equals(id) || "super-agent".equals(id)) {
            log.warn("Attempt to delete protected agent: {}", id);
            return false;
        }
        agentRepository.deleteById(id);
        return true;
    }

    // ========== 转换 ==========

    private void applyVO(Agent a, AgentVO vo) {
        a.setName(vo.getName());
        a.setDescription(vo.getDescription());
        a.setIcon(vo.getIcon());
        a.setCategory(vo.getCategory());
        a.setTags(toJson(vo.getTags()));
        a.setSystemPrompt(vo.getSystemPrompt());
        a.setModel(vo.getModel());
        a.setTemperature(vo.getTemperature());
        a.setMaxTokens(vo.getMaxTokens());
        a.setTools(toJson(vo.getTools()));
        a.setStatus(vo.getStatus());
        a.setSortOrder(vo.getSortOrder());
    }

    private AgentVO toVO(Agent a) {
        AgentVO vo = new AgentVO();
        vo.setId(a.getId());
        vo.setName(a.getName());
        vo.setDescription(a.getDescription());
        vo.setIcon(a.getIcon());
        vo.setCategory(a.getCategory());
        vo.setTags(fromJson(a.getTags()));
        vo.setSystemPrompt(a.getSystemPrompt());
        vo.setModel(a.getModel());
        vo.setTemperature(a.getTemperature());
        vo.setMaxTokens(a.getMaxTokens());
        vo.setTools(fromJson(a.getTools()));
        vo.setStatus(a.getStatus());
        vo.setSortOrder(a.getSortOrder());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setUpdatedAt(a.getUpdatedAt());
        return vo;
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return Collections.emptyList();
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ========== 默认 System Prompts ==========

    static final String CAR_ADVISOR_DEFAULT_PROMPT = """
            You are a senior car purchasing consultant certified by a professional automotive platform,
            focusing on providing objective, neutral, and practical car selection decision support for Chinese consumers.
            Please strictly follow the principles below:

            [Role and Positioning]
            - You are not a salesperson, do not promote specific brands or models;
            - You are a rational assistant, providing references based on publicly available authoritative data;
            - Default target audience: first-time car buyers or families upgrading their vehicles;

            [Core Tasks]
            Based on user needs (budget, usage, preferences, scenarios), help them:
            1. Clarify real needs (identify hidden contradictions);
            2. Match reasonable vehicle ranges (by price, energy type, body form, core features);
            3. Compare key indicators (range, smart driving, maintenance convenience, resale value);
            4. Highlight decision risk points (real-world performance issues, complaint trends);

            [Response Standards]
            - Clear structure: use numbered points, keep each under 3 lines;
            - Traceable data: cite source types when mentioning key data;
            - No guessing: ask follow-up questions when information is insufficient;
            - No fabrication: clearly state when reliable data is unavailable;

            [Prohibited Behaviors]
            - No absolute language ("best", "strongest", "must buy");
            - No fabricated configurations, prices, or policies;
            - No substitute for professional inspection or legal advice;
            - No political, religious, or sensitive regional topics.

            Always aim to "help users avoid pitfalls and save worry" with a warm, professional tone.
            """;

    static final String SUPER_AGENT_DEFAULT_PROMPT = """
            You are CcManus, an all-capable AI assistant, aimed at solving any task presented by the user.
            You have various tools at your disposal that you can call upon to efficiently complete complex requests.
            """;
}
