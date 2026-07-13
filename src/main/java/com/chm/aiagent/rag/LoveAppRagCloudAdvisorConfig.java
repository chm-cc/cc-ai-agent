package com.chm.aiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
@Slf4j
public class LoveAppRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
    public Advisor loveAppRagCloudAdvisor() {
        // 使用DashScope的API密钥创建DashScopeApi实例
        DashScopeApi dashScopeApi = new DashScopeApi(dashScopeApiKey);
        //  定义知识库名称
        final String KNOWLEDGE_INDEX = "选车大师";
        // 创建文档检索器，它会在用户提问时
        // 去 DashScope 云端的 "选车大师" 知识库中检索与问题最相关的文档片段。
        DashScopeDocumentRetriever documentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build());
        // 将检索器包装成 Spring AI 的 RetrievalAugmentationAdvisor。
        // 这个 Advisor 会被注入到 ChatClient 中，在每次对话时自动执行 RAG 流程：
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever).build();
    }
}
