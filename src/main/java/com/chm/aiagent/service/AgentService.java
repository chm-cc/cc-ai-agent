package com.chm.aiagent.service;

import com.chm.aiagent.config.AgentConfig;
import com.chm.aiagent.dto.AgentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentConfig agentConfig;

    public List<AgentVO> listActive() {
        return agentConfig.getActiveAgents();
    }

    public AgentVO getById(String agentId) {
        return agentConfig.getAgent(agentId);
    }
}
