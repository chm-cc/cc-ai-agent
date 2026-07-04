
package com.chm.aiagent.rag;

import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建自定义的 RAG 检索增强顾问的工厂
 */
public class LoveAppRagCustomAdvisorFactory {

    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String keyword) {
        // 构建过滤表达式，按元数据字段（如 keywords）筛选文档
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        var filterExpression = filterBuilder.eq("filename", keyword).build();

        // 文档检索器，从 VectorStore 中检索相关文档
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                // 每次检索返回最相关的 5 个文档块
                .topK(5)
                .filterExpression(filterExpression)
                .build();

        // 将检索器包装为 Advisor，嵌入 ChatClient 的 RAG 流程
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
}
