package com.chm.aiagent.controller;

import com.chm.aiagent.common.Result;
import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.service.AgentService;
import jakarta.validation.Valid;
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

    @GetMapping("/all")
    public Result<List<AgentVO>> listAll() {
        return Result.ok(agentService.listAll());
    }

    @GetMapping("/{agentId}")
    public Result<AgentVO> get(@PathVariable String agentId) {
        AgentVO agent = agentService.getById(agentId);
        if (agent == null) {
            return Result.fail(10004, "Agent 不存在");
        }
        return Result.ok(agent);
    }

    @PostMapping
    public Result<AgentVO> create(@Valid @RequestBody AgentVO req) {
        if (req.getId() == null || req.getId().isBlank()) {
            return Result.fail(10001, "Agent ID 不能为空");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            return Result.fail(10001, "Agent 名称不能为空");
        }
        if (req.getSystemPrompt() == null || req.getSystemPrompt().isBlank()) {
            return Result.fail(10001, "System Prompt 不能为空，请描述 Agent 的功能和行为规范");
        }
        AgentVO existing = agentService.getById(req.getId());
        if (existing != null) {
            return Result.fail(10001, "Agent ID 已存在: " + req.getId());
        }
        return Result.ok(agentService.create(req));
    }

    @PutMapping("/{agentId}")
    public Result<AgentVO> update(@PathVariable String agentId, @RequestBody AgentVO req) {
        AgentVO updated = agentService.update(agentId, req);
        if (updated == null) {
            return Result.fail(10004, "Agent 不存在");
        }
        return Result.ok(updated);
    }

    @DeleteMapping("/{agentId}")
    public Result<Void> delete(@PathVariable String agentId) {
        boolean deleted = agentService.delete(agentId);
        if (!deleted) {
            return Result.fail(10001, "无法删除该 Agent（car-advisor 和 super-agent 为系统保留 Agent）");
        }
        return Result.ok();
    }
}
