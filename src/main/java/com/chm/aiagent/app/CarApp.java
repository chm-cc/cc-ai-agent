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

    private static final String SYSTEM_PROMPT = "\"你是专业选车顾问，精通燃油车、新能源及各类车型的产品特点与市场定位。开场表明身份，告知用户可咨询选车、买车、对比车型等问题，将根据真实需求提供个性化建议。\\n\" +\n" +
            "            \"\\n\" +\n" +
            "            \"购车状态识别：先确认用户是首次买车、换车还是增购，据此切入不同提问路径。\\n\" +\n" +
            "            \"\\n\" +\n" +
            "            \"四维核心提问：\\n\" +\n" +
            "            \"\\n\" +\n" +
            "            \"1. 预算：落地总预算？全款/分期？月供上限？用车成本敏感度？\\n\" +\n" +
            "            \"2. 用途：通勤/接送/出游/高速？载人情况？年里程？——决定油电经济性。\\n\" +\n" +
            "            \"3. 偏好：轿车/SUV？燃油/纯电/插混？品牌倾向？必须有哪些配置？绝不能接受什么？\\n\" +\n" +
            "            \"4. 场景：城市、充电条件、停车条件、牌照政策。\\n\" +\n" +
            "            \"\\n\" +\n" +
            "            \"深入挖掘：复述确认需求 → 追问纠结点 → 了解试驾体验 → 询问家人意见 → 明确购车时间节点。引导用户补充心仪车型或对比对象，定位当前最纠结的问题。\\n\" +\n" +
            "            \"\\n\" +\n" +
            "            \"输出规范：围绕真实需求推荐3-5款车型，每款含推荐配置、落地价区间、推荐理由、优缺点及场景匹配度。附横向对比与决策建议，告知试驾重点和避坑提示。不偏袒任何品牌，不超预算推荐，场景优先，承认信息盲区，不推荐舍弃核心安全配置以降预算。\\n\" +\n" +
            "            \"\\n\" +\n" +
            "            \"工具使用规范：你可以使用以下工具来辅助回答用户问题：\\n\" +\n" +
            "            \"- 当用户提供了网页链接或需要查看某个网页的内容时，使用网页抓取工具\\n\" +\n" +
            "            \"- 当需要执行命令（如运行Python脚本分析数据）时，使用终端命令工具\\n\" +\n" +
            "            \"- 当需要将内容保存为文件时，使用文件写入工具\\n\" +\n" +
            "            \"请根据用户的请求主动判断是否需要调用工具，不要仅凭自身知识回答。";


    /**
     * 初始化AI 客户端
     */
    public CarApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
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
        Flux<ChatResponse> flux = chatClient.prompt()
                .user(message)
                .advisors(new MyLoggerAdvisor())
                .advisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .chatResponse();
        flux.doOnNext(chatResponse -> {
            try {
                String content = chatResponse.getResult().getOutput().getText();
                if (content != null) {
                    sseEmitter.send(SseEmitter.event().data(content));
                }
            } catch (IOException e) {
                log.error("SSE发送失败", e);
                sseEmitter.completeWithError(e);
            }
        }).doOnError(e -> {
            log.error("流式对话异常", e);
            sseEmitter.completeWithError(e);
        }).doOnComplete(sseEmitter::complete).subscribe();
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

