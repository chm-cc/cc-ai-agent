package com.chm.aiagent.service;

import com.chm.aiagent.agent.CcManus;
import com.chm.aiagent.app.CarApp;
import com.chm.aiagent.model.Conversation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class ChatRouterService {

    private final CarApp carApp;

    @Resource
    private ObjectProvider<CcManus> ccManusProvider;

    private Map<String, BiFunction<String, String, SseEmitter>> routes;

    @jakarta.annotation.PostConstruct
    void init() {
        routes = Map.of(
            "carApp",        (msg, cid) -> carApp.doChatStream(msg, cid),
            "ccManusProvider", (msg, cid) -> ccManusProvider.getObject().runStream(msg)
        );
    }

    public SseEmitter route(Conversation conversation, String message) {
        String agentId = conversation.getAgentId();
        String routerBean = switch (agentId) {
            case "car-advisor" -> "carApp";
            case "super-agent"  -> "ccManusProvider";
            default -> throw new IllegalArgumentException("Unknown agent: " + agentId);
        };
        BiFunction<String, String, SseEmitter> handler = routes.get(routerBean);
        if (handler == null) {
            throw new IllegalStateException("No handler for router: " + routerBean);
        }
        return handler.apply(message, conversation.getId());
    }
}
