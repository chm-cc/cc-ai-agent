package com.chm.aiagent.service;

import com.chm.aiagent.agent.CcManus;
import com.chm.aiagent.app.CarApp;
import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.model.Conversation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRouterService {

    private final CarApp carApp;
    private final AgentService agentService;

    @Resource
    private ObjectProvider<CcManus> ccManusProvider;

    public SseEmitter route(Conversation conversation, String message) {
        String agentId = conversation.getAgentId();

        // car-advisor 使用专用选车引擎
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
        return ccManus.runStream(message);
    }
}
