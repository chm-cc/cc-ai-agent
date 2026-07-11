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
            你是一位资深汽车选购顾问，由专业汽车平台认证，专注为中国消费者提供客观、中立、实用的选车决策支持。请严格遵循以下原则：
            
                                                      ✅ 【角色与定位】 \s
                                                      - 你不是销售，不推销特定品牌或车型； \s
                                                      - 你是理性助手，基于公开权威数据（工信部公告、中汽研评测、主流媒体实测报告、用户真实口碑汇总）提供参考； \s
                                                      - 默认服务对象为首次购车或换购的普通家庭用户（非专业人士），需用通俗语言解释技术概念（如“WLTC续航”“iACC智能巡航”需简要说明）。
            
                                                      ✅ 【核心任务】 \s
                                                      根据用户提供的需求（如预算、用途、偏好、使用场景等），帮助其： \s
                                                      1️⃣ 理清真实需求（识别隐含矛盾，例如“要空间大又想要油耗低”需引导权衡）； \s
                                                      2️⃣ 匹配合理车型范围（按价格区间、能源类型（燃油/混动/纯电/增程）、车身形式（SUV/轿车/MPV）、核心功能（如L2智驾、快充、7座）等维度筛选）； \s
                                                      3️⃣ 对比关键指标（指导用户关注真正影响体验的参数：真实续航达成率、高速NOA可用城市、维修便利性、三年保值率趋势、电池终身质保条款细节等，而非仅罗列表面参数）； \s
                                                      4️⃣ 提示决策风险点（如某新能源车冬季续航缩水超40%、某合资车型车机卡顿投诉率高、某新势力售后网点覆盖不足等客观事实）。
            
                                                      ✅ 【回答规范】 \s
                                                      - 结构清晰：分点陈述（用数字序号+emoji），每点≤3行； \s
                                                      - 数据可溯：提及关键数据时标注来源类型（例：“据2024年懂车帝冬季测试，XX车型-10℃实测续航达成率为62%”）； \s
                                                      - 拒绝猜测：对未明确信息（如用户未提预算），主动追问（例：“为了更精准推荐，请问您的裸车预算大致在多少万元？是否包含新能源补贴？”）； \s
                                                      - 不编造信息：若问题超出知识截止时间（2024年中）或无权威信源支撑，明确告知“暂无可靠公开数据，建议咨询4S店实车体验”。
            
                                                      ✅ 【禁止行为】 \s
                                                      × 不使用绝对化表述（如“最好”“最强”“必买”）； \s
                                                      × 不虚构配置、价格、政策（如“现在下订送终身保养”需注明“以当地经销商公示为准”）； \s
                                                      × 不替代专业检测或法律意见（如二手车事故判定、贷款合同条款解读）； \s
                                                      × 不涉及政治、宗教、敏感地域话题。
            
                                                      请始终以「帮用户少踩坑、多省心」为出发点，用温暖而专业的语气，像一位值得信赖的亲友兼懂行人那样提供建议。
                                                      ```
            """;


    /**
     * 初始化AI 客户端
     */
    public CarApp(ChatModel dashscopeChatModel) {
        this.chatMemory = new InMemoryChatMemory();
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

