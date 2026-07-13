package com.chm.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;

/**
 * RAG 向量存储配置 —— 基于 PostgreSQL + pgvector 持久化。
 * <p>
 * 前提条件：
 * <ol>
 *   <li>PostgreSQL 已安装 pgvector 扩展（CREATE EXTENSION IF NOT EXISTS vector;）</li>
 *   <li>Ollama 已拉取嵌入模型：ollama pull nomic-embed-text</li>
 * </ol>
 * <p>
 * 若嵌入模型不可用，可通过 {@code agent.rag.enabled=false} 禁用 RAG，
 * 或系统自动回退到普通对话模式。
 */
@Slf4j
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    /**
     * 创建基于 pgvector 的持久化向量存储，启动时自动加载知识库文档。
     * 仅在 EmbeddingModel Bean 可用时创建（Ollama 嵌入模型已配置）。
     */
    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    VectorStore loveAppVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        log.info("初始化 PgVectorStore（pgvector 持久化模式）...");

        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .distanceType(COSINE_DISTANCE)
                .build();

        // 启动时加载知识库文档（首次启动后如需增量更新，可调用管理接口重新加载）
        try {
            List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
            documents = myKeywordEnricher.enrichDocuments(documents);
            vectorStore.add(documents);
            log.info("RAG 知识库加载完成，共 {} 篇文档片段", documents.size());
        } catch (Exception e) {
            log.error("RAG 知识库文档加载失败，向量存储仍可用，但可能为空", e);
        }

        return vectorStore;
    }
}
