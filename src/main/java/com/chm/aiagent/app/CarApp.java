package com.chm.aiagent.app;

import com.chm.aiagent.advisor.MyLoggerAdvisor;
import com.chm.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class CarApp {

    private final ChatClient chatClient;

    /** 共享对话记忆，流式和非流式路径统一使用 */
    private final ChatMemory chatMemory;

    private static final String SYSTEM_PROMPT = """
            你是专业选车顾问，熟悉燃油车与新能源车型市场与产品特点，用户可咨询选车、购车与对比车型等问题。你必须始终遵循\u201c先回答后提问\u201d原则：先直接回答用户当前问题，再做简要解释补充，最后才进行轻量追问，禁止用连续提问替代回答。
            信息补全基于四维：预算（落地价/月供）、用途（通勤/出游/里程）、偏好（轿车/SUV/油电/品牌）、场景（城市/充电/政策）。仅在关键信息缺失影响推荐时补充询问。
            输出结构：结论优先 → 简要说明 → 3-5款车型推荐（含价格/优缺点/适配场景）→ 最多3个选择式追问。要求不预设立场，不过度追问，在信息不完整情况下仍需给出可执行建议。你拥有多种可调用的工具，能够高效完成复杂的请求。
            """;


    /**
     * 初始化AI 客户端
     */
    public CarApp(ChatModel ollamaChatModel) {
        this.chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
                )
                .build();
    }


    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message).advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
//        log.info("content: {}", content);
        return content;
    }

    public SseEmitter doChatStream(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(1000000L);

        // 客户端断开标记，volatile 数组保证跨线程可见
        final boolean[] disconnected = {false};

        sseEmitter.onCompletion(() -> disconnected[0] = true);
        sseEmitter.onTimeout(() -> {
            disconnected[0] = true;
            log.warn("SSE 连接超时");
        });

        Flux<ChatResponse> flux = chatClient.prompt()
                .user(message)
                .advisors(new MyLoggerAdvisor())
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .chatResponse()
                // 客户端断开后立即终止 Flux，不再消耗 LLM token
                .takeWhile(resp -> !disconnected[0]);

        flux.doOnNext(chatResponse -> {
            try {
                String content = chatResponse.getResult().getOutput().getText();
                if (content != null && !disconnected[0]) {
                    sseEmitter.send(SseEmitter.event().data(content));
                }
            } catch (IOException e) {
                // SSE 发送失败 = 客户端断开，设标记终止 Flux
                disconnected[0] = true;
                log.info("客户端已断开，停止流式输出");
            }
        }).doOnComplete(() -> {
            if (!disconnected[0]) {
                sseEmitter.complete();
            }
        }).doOnError(e -> {
            if (!disconnected[0]) {
                log.error("流式对话异常", e);
                sseEmitter.completeWithError(e);
            }
        }).subscribe();

        return sseEmitter;
    }
    record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 聊天并生成恋爱报告
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        ChatResponse chatResponse = chatClient
                .prompt()
                // 开启日志，便于观察效果
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
//                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
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

    /**
     * 结合工具（向量知识库）进行对话
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
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
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}

