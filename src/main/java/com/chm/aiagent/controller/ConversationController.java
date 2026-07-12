package com.chm.aiagent.controller;

import com.chm.aiagent.common.Result;
import com.chm.aiagent.dto.*;
import com.chm.aiagent.model.Conversation;
import com.chm.aiagent.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatRouterService chatRouterService;

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "admin";
    }

    @PostMapping
    public Result<ConversationVO> create(@Valid @RequestBody CreateConversationRequest req) {
        return Result.ok(conversationService.create(req.getAgentId(), currentUserId(), req.getTitle()));
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = currentUserId();
        List<ConversationVO> list = conversationService.list(userId, agentId, page, size);
        int total = conversationService.count(userId, agentId);
        return Result.ok(Map.of("list", list, "total", total, "page", page, "size", size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        conversationService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/messages")
    public Result<Map<String, Object>> messages(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<MessageVO> list = messageService.list(id, page, size);
        int total = messageService.count(id);
        return Result.ok(Map.of("list", list, "total", total, "page", page, "size", size));
    }

    @PostMapping("/{id}/chat/stream")
    public SseEmitter chatStream(@PathVariable String id, @Valid @RequestBody ChatRequest req) {
        Conversation conv = conversationService.getEntity(id);
        if (conv == null) {
            SseEmitter bad = new SseEmitter();
            bad.completeWithError(new IllegalArgumentException("会话不存在: " + id));
            return bad;
        }

        // 保存用户消息
        messageService.save(id, "USER", req.getMessage());
        // 更新会话摘要
        String preview = req.getMessage().length() > 100
                ? req.getMessage().substring(0, 100) + "..." : req.getMessage();
        conversationService.touchAfterMessage(id, preview);

        // 路由到对应的 AI 引擎，直接返回 SseEmitter
        return chatRouterService.route(conv, req.getMessage());
    }
}
