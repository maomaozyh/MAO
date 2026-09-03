package com.mao.maocodemother.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Milvus 向量数据库装配。
 * 通过 application.yml 中的 milvus.* 配置连接 standalone Milvus，
 * 并复用 LangChain4j 自动装配的 EmbeddingModel 进行文本向量化。
 *
 * <p>优化项：显式指定索引类型（默认 HNSW）与度量方式（默认 COSINE），
 * 相比 Milvus 默认配置拥有更高的召回率与更稳定的检索性能。
 * （注：当前 LangChain4j fork 的 MilvusEmbeddingStore Builder 仅接受
 * {@link IndexType} / {@link MetricType} 枚举，且不支持 extraParam。）
 */
@Configuration
public class MilvusVectorStoreConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.collection-name:yu_ai_knowledge}")
    private String collectionName;

    @Value("${milvus.dimension:1024}")
    private int dimension;

    @Value("${milvus.index-type:HNSW}")
    private String indexType;

    @Value("${milvus.metric-type:COSINE}")
    private String metricType;

    @Bean
    @Lazy
    public MilvusEmbeddingStore milvusEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .dimension(dimension)
                .indexType(IndexType.valueOf(indexType.toUpperCase()))
                .metricType(MetricType.valueOf(metricType.toUpperCase()))
                .build();
    }
}
