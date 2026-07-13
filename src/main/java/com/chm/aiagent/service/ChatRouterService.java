package com.chm.aiagent.service;

import com.chm.aiagent.agent.CcManus;
import com.chm.aiagent.app.CarApp;
import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.model.Conversation;
import com.chm.aiagent.model.Message;
import com.chm.aiagent.repository.MessageRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRouterService {

    private final CarApp carApp;
    private final AgentService agentService;
    private final MessageRepository messageRepository;

    @Resource
    private ObjectProvider<CcManus> ccManusProvider;

    /** 加载到 Agent 上下文的历史消息窗口大小 */
    private static final int HISTORY_WINDOW_SIZE = 20;

    public SseEmitter route(Conversation conversation, String message) {
        String agentId = conversation.getAgentId();

        // car-advisor 使用专用选车引擎（CarApp 内置 buildContext 加载历史）
        if ("car-advisor".equals(agentId)) {
            return carApp.doChatStream(message, conversation.getId());
        }

        // 其余 Agent 统一走 CcManus ReAct 引擎
        // 从数据库获取该 Agent 的 systemPrompt 进行动态配置
        AgentVO agent = agentService.getById(agentId);
        CcManus ccManus = ccManusProvider.getObject();
        if (agent != null && agent.getSystemPrompt() != null) {
            ccManus.configure(agent.getSystemPrompt());
        }

        // 加载历史对话到 Agent 上下文，使多轮对话有记忆
        loadConversationHistory(ccManus, conversation.getId(), message);

        return ccManus.runStream(message);
    }

    /**
     * 从数据库加载历史消息，预填充到 Agent 的 messageList 中。
     * 排除当前消息（已由 controller 存入 DB），避免重复。
     */
    private void loadConversationHistory(CcManus ccManus, String conversationId, String currentMessage) {
        try {
            List<Message> history = messageRepository.findRecentByConversation(conversationId, HISTORY_WINDOW_SIZE);
            if (history.isEmpty()) return;

            List<org.springframework.ai.chat.messages.Message> context = new ArrayList<>();
            for (Message m : history) {
                // 跳过当前用户消息（runStream 会自行添加）
                if ("USER".equals(m.getRole()) && m.getContent().equals(currentMessage)) {
                    continue;
                }
                if ("USER".equals(m.getRole())) {
                    context.add(new UserMessage(m.getContent()));
                } else if ("ASSISTANT".equals(m.getRole())) {
                    context.add(new AssistantMessage(m.getContent()));
                }
                // 跳过 TOOL 角色的消息（ReAct 中间产物，不重放）
            }

            if (!context.isEmpty()) {
                ccManus.getMessageList().addAll(context);
                log.info("已加载 {} 条历史消息到 Agent 上下文（conversationId={}）", context.size(), conversationId);
            }
        } catch (Exception e) {
            log.warn("加载历史消息失败，Agent 将以无记忆模式运行: {}", e.getMessage());
        }
    }
}
