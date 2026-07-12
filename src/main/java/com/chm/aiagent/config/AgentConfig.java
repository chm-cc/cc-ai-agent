package com.chm.aiagent.config;

import com.chm.aiagent.dto.AgentVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class AgentConfig {
    private List<AgentVO> agents = new ArrayList<>();

    public List<AgentVO> getActiveAgents() {
        if (agents == null) return List.of();
        return agents.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .sorted(Comparator.comparingInt(agents::indexOf))
                .toList();
    }

    public AgentVO getAgent(String agentId) {
        if (agents == null) return null;
        return agents.stream()
                .filter(a -> agentId.equals(a.getId()) && "ACTIVE".equals(a.getStatus()))
                .findFirst()
                .orElse(null);
    }
}
