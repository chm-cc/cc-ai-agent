package com.chm.aiagent.app;

import com.chm.aiagent.advisor.MyLoggerAdvisor;
import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.model.Message;
import com.chm.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.chm.aiagent.repository.MessageRepository;
import com.chm.aiagent.service.AgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CarApp {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    @Resource
    private MessageRepository messageRepository;

    @Resource
    private AgentService agentService;

    /** DB sliding window: max history messages loaded per request */
    private static final int MEMORY_WINDOW_SIZE = 20;

    /** Fallback system prompt if agent not found in DB */
    private static final String FALLBACK_SYSTEM_PROMPT = """
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

    public CarApp(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(FALLBACK_SYSTEM_PROMPT)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }

    /** 从 agents 表读取 car-advisor 的 System Prompt */
    private String getSystemPrompt() {
        try {
            AgentVO agent = agentService.getById("car-advisor");
            if (agent != null && agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank()) {
                return agent.getSystemPrompt();
            }
        } catch (Exception e) {
            log.warn("Failed to load car-advisor system prompt from DB, using fallback", e);
        }
        return FALLBACK_SYSTEM_PROMPT;
    }

    /**
     * Generate a short conversation title based on the first exchange.
     */
    public String generateTitle(String userMessage, String assistantMessage) {
        String prompt = String.format(
            "根据以下对话内容，生成一个简短的中文标题（不超过15个字），直接输出标题不要带引号或解释。%n用户：%s%n助手：%s",
            userMessage,
            assistantMessage != null && assistantMessage.length() > 200
                ? assistantMessage.substring(0, 200)
                : assistantMessage
        );
        try {
            org.springframework.ai.chat.prompt.Prompt aiPrompt =
                new org.springframework.ai.chat.prompt.Prompt(new org.springframework.ai.chat.messages.UserMessage(prompt));
            var response = chatModel.call(aiPrompt);
            String title = response.getResult().getOutput().getText().trim();
            title = title.replaceAll("^[\"'「]|[\"'」]$", "").trim();
            return title.length() > 20 ? title.substring(0, 20) : title;
        } catch (Exception e) {
            log.warn("Failed to generate title", e);
            return null;
        }
    }

    /**
     * Build LLM context by loading recent N messages from DB (sliding window).
     */
    private List<org.springframework.ai.chat.messages.Message> buildContext(String conversationId, String currentMessage) {
        List<Message> history = messageRepository.findRecentByConversation(conversationId, MEMORY_WINDOW_SIZE);

        String systemPrompt = getSystemPrompt();
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));

        for (Message m : history) {
            if ("USER".equals(m.getRole()) && m.getContent().equals(currentMessage)) {
                continue;
            }
            if ("USER".equals(m.getRole())) {
                messages.add(new org.springframework.ai.chat.messages.UserMessage(m.getContent()));
            } else if ("ASSISTANT".equals(m.getRole())) {
                messages.add(new org.springframework.ai.chat.messages.AssistantMessage(m.getContent()));
            }
        }

        messages.add(new org.springframework.ai.chat.messages.UserMessage(currentMessage));
        return messages;
    }

    public String doChat(String message, String chatId) {
        List<org.springframework.ai.chat.messages.Message> context = buildContext(chatId, message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .messages(context)
                .advisors(new MyLoggerAdvisor())
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    public SseEmitter doChatStream(String message, String chatId) {
        List<org.springframework.ai.chat.messages.Message> context = buildContext(chatId, message);

        SseEmitter sseEmitter = new SseEmitter(1000000L);

        final boolean[] disconnected = {false};

        sseEmitter.onCompletion(() -> disconnected[0] = true);
        sseEmitter.onTimeout(() -> {
            disconnected[0] = true;
            log.warn("SSE timeout");
        });

        Flux<ChatResponse> flux = chatClient.prompt()
                .messages(context)
                .advisors(new MyLoggerAdvisor())
                .stream()
                .chatResponse()
                .takeWhile(resp -> !disconnected[0]);

        flux.doOnNext(chatResponse -> {
            try {
                String content = chatResponse.getResult().getOutput().getText();
                if (content != null && !disconnected[0]) {
                    sseEmitter.send(SseEmitter.event().data(content));
                }
            } catch (IOException e) {
                disconnected[0] = true;
                log.info("Client disconnected, stopping stream");
            }
        }).doOnComplete(() -> {
            if (!disconnected[0]) {
                sseEmitter.complete();
            }
        }).doOnError(e -> {
            if (!disconnected[0]) {
                log.error("Stream error", e);
                sseEmitter.completeWithError(e);
            }
        }).subscribe();

        return sseEmitter;
    }

    // ============================================================
    // Legacy methods
    // ============================================================

    record LoveReport(String title, List<String> suggestions) {}

    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(getSystemPrompt())
                .user(message)
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // RAG 模式：loveAppVectorStore 由 LoveAppVectorStoreConfig 提供（pgvector 持久化）
    @Autowired(required = false)
    private VectorStore loveAppVectorStore;

    @Autowired(required = false)
    private Advisor loveAppRagCloudAdvisor;

    @Autowired(required = false)
    private VectorStore pgVectorVectorStore;

    public String doChatWithRag(String message, String chatId) {
        if (loveAppVectorStore == null) {
            log.warn("RAG 模式不可用：EmbeddingModel Bean 未注册（请检查 spring.ai.ollama.embedding.model 配置及 nomic-embed-text 模型是否已拉取），回退到普通对话");
            return doChat(message, chatId);
        }
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(new MyLoggerAdvisor())
                .advisors(
                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                                loveAppVectorStore, "购车问答-换车篇.md"
                        )
                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
