package com.chm.aiagent.controller;

import com.chm.aiagent.common.Result;
import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public Result<List<AgentVO>> list() {
        return Result.ok(agentService.listActive());
    }

    @GetMapping("/{agentId}")
    public Result<AgentVO> get(@PathVariable String agentId) {
        AgentVO agent = agentService.getById(agentId);
        if (agent == null) {
            return Result.fail(10004, "Agent 不存在");
        }
        return Result.ok(agent);
    }
}
