package com.chm.aiagent.controller;

import com.chm.aiagent.app.CarApp;
import com.chm.aiagent.common.Result;
import com.chm.aiagent.dto.*;
import com.chm.aiagent.exception.BusinessException;
import com.chm.aiagent.exception.ErrorCode;
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
    private final CarApp carApp;

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }

    /** 校验会话归属，防止跨用户访问 */
    private void assertConversationOwner(Conversation conv, String userId) {
        if (conv != null && conv.getUserId() != null && !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此会话");
        }
    }

    @PostMapping
    public Result<ConversationVO> create(@Valid @RequestBody CreateConversationRequest req) {
        return Result.ok(conversationService.create(req.getAgentId(), currentUserId(), req.getTitle()));
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam String agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = currentUserId();
        List<ConversationVO> list = conversationService.list(userId, agentId, page, size);
        int total = conversationService.count(userId, agentId);
        return Result.ok(Map.of("list", list, "total", total, "page", page, "size", size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        String userId = currentUserId();
        Conversation conv = conversationService.getEntity(id);
        assertConversationOwner(conv, userId);
        conversationService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> rename(@PathVariable String id, @RequestBody RenameRequest req) {
        String userId = currentUserId();
        Conversation conv = conversationService.getEntity(id);
        assertConversationOwner(conv, userId);
        conversationService.rename(id, req.getTitle());
        return Result.ok();
    }

    @PostMapping("/{id}/messages")
    public Result<Void> saveMessage(@PathVariable String id, @RequestBody SaveMessageRequest req) {
        String userId = currentUserId();
        messageService.save(id, userId, req.getRole(), req.getContent());
        return Result.ok();
    }

    @GetMapping("/{id}/messages")
    public Result<Map<String, Object>> messages(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        String userId = currentUserId();
        Conversation conv = conversationService.getEntity(id);
        assertConversationOwner(conv, userId);
        List<MessageVO> list = messageService.list(id, page, size);
        int total = messageService.count(id);
        return Result.ok(Map.of("list", list, "total", total, "page", page, "size", size));
    }

    @PutMapping("/{id}/messages/{msgId}/feedback")
    public Result<Void> feedback(@PathVariable String id, @PathVariable Long msgId, @RequestBody FeedbackRequest req) {
        String userId = currentUserId();
        Conversation conv = conversationService.getEntity(id);
        assertConversationOwner(conv, userId);
        messageService.updateFeedback(msgId, req.getFeedback());
        return Result.ok();
    }

    @PostMapping("/{id}/generate-title")
    public Result<String> generateTitle(@PathVariable String id) {
        String userId = currentUserId();
        Conversation conv = conversationService.getEntity(id);
        assertConversationOwner(conv, userId);
        List<MessageVO> messages = messageService.list(id, 1, 2);
        if (messages.isEmpty()) {
            return Result.ok(null);
        }
        String userMsg = messages.stream()
            .filter(m -> "USER".equals(m.getRole()))
            .findFirst().map(MessageVO::getContent).orElse("");
        String assistantMsg = messages.stream()
            .filter(m -> "ASSISTANT".equals(m.getRole()))
            .findFirst().map(MessageVO::getContent).orElse("");

        // 即使只有 USER 消息（CcManus content 可能为空），也基于用户问题生成标题
        String title = carApp.generateTitle(userMsg, assistantMsg);
        if (title != null && !title.isBlank()) {
            conversationService.rename(id, title);
        }
        return Result.ok(title);
    }

    @PostMapping("/{id}/chat/stream")
    public SseEmitter chatStream(@PathVariable String id, @Valid @RequestBody ChatRequest req) {
        String userId = currentUserId();
        Conversation conv = conversationService.getEntity(id);
        if (conv == null) {
            SseEmitter bad = new SseEmitter();
            bad.completeWithError(new IllegalArgumentException("会话不存在: " + id));
            return bad;
        }
        assertConversationOwner(conv, userId);

        // 保存用户消息（带 userId）
        messageService.save(id, userId, "USER", req.getMessage());
        // 更新会话摘要
        String preview = req.getMessage().length() > 100
                ? req.getMessage().substring(0, 100) + "..." : req.getMessage();
        conversationService.touchAfterMessage(id, preview);

        // 路由到对应的 AI 引擎，直接返回 SseEmitter
        return chatRouterService.route(conv, req.getMessage());
    }
}
