package com.mao.maocodemother.ai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 Milvus 的向量检索服务：素材/技能/文档的入库与语义检索。
 *
 * <p>MilvusEmbeddingStore 本身不做文本向量化，这里统一通过 EmbeddingModel 先向量化再写入，
 * 检索时同样对 query 做向量化后做相似度搜索。
 *
 * <p>优化点（相对旧实现）：
 * <ul>
 *   <li>幂等入库：以 {@code type:bizId} 作为向量主键，重复入库自动覆盖，避免重复向量；</li>
 *   <li>长文本分块：单段超过 {@link #CHUNK_SIZE} 字符自动切分为多个 chunk，提升检索召回粒度；</li>
 *   <li>分数阈值 + 类型过滤：检索可按 minScore / type 过滤，剔除低相关与跨类型噪声；</li>
 *   <li>嵌入缓存：相同文本复用 Embedding，减少 Embedding API 调用；</li>
 *   <li>暴露 ContentRetriever，供 AiServices 做检索增强（RAG）。</li>
 * </ul>
 */
@Slf4j
@Service
public class VectorSearchService {

    /** 单段文本超过该字符数则切片（过长文本切片可提升召回粒度） */
    private static final int CHUNK_SIZE = 800;
    /** remove 时最多尝试删除的分片数（覆盖绝大多数长文本分块场景） */
    private static final int MAX_CHUNK_REMOVE = 128;

    private final MilvusEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;

    /** 嵌入结果缓存：相同文本复用向量，降低 Embedding API 成本 */
    private final Cache<String, Embedding> embedCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    public VectorSearchService(@Lazy MilvusEmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    // ===================== 入库 =====================

    /**
     * 幂等入库：按业务 id（结合 type 命名空间）去重后写入，长文本自动分块。
     *
     * @param bizId     业务主键（如素材ID / 技能ID / 应用ID）
     * @param text      待入库文本
     * @param metadata  业务元数据（需包含 {@code type} 字段，如 material / skill / doc）
     */
    public void upsert(String bizId, String text, Map<String, Object> metadata) {
        String type = metadata == null ? "unknown" : String.valueOf(metadata.getOrDefault("type", "unknown"));
        String baseId = type + ":" + bizId;
        // 先清理旧向量（含可能的分块），避免重复
        remove(baseId);

        List<String> chunks = splitChunks(text);
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = chunks.size() == 1 ? baseId : baseId + "_c" + i;
            Map<String, Object> meta = new HashMap<>();
            if (metadata != null) {
                meta.putAll(metadata);
            }
            meta.put("row_id", chunkId);
            meta.put("biz_id", bizId);
            meta.put("type", type);
            meta.put("chunk_index", i);
            meta.put("chunk_total", chunks.size());
            Embedding embedding = embed(chunks.get(i));
            embeddingStore.addAll(List.of(chunkId), List.of(embedding),
                    List.of(TextSegment.from(chunks.get(i), Metadata.from(meta))));
        }
        log.debug("向量入库完成 bizId={} type={} chunks={}", bizId, type, chunks.size());
    }

    /**
     * 指定 id 入库（兼容旧调用方），内部转调 {@link #upsert(String, String, Map)} 实现幂等。
     */
    public void ingest(String id, String text, Map<String, Object> metadata) {
        upsert(id, text, metadata);
    }

    /**
     * 自动生成 id 入库（兼容旧调用方）。
     */
    public void ingest(String text, Map<String, Object> metadata) {
        upsert(UUID.randomUUID().toString(), text, metadata);
    }

    /**
     * 按业务 id 删除该实体的全部向量（含分块）。
     */
    public void remove(String bizId) {
        List<String> ids = new ArrayList<>(MAX_CHUNK_REMOVE + 1);
        ids.add(bizId);
        for (int i = 0; i < MAX_CHUNK_REMOVE; i++) {
            ids.add(bizId + "_c" + i);
        }
        try {
            embeddingStore.removeAll(ids);
        } catch (Exception e) {
            // 向量不存在时忽略；Milvus 不可用时仅记录，不影响主流程
            log.debug("向量删除跳过 bizId={}: {}", bizId, e.getMessage());
        }
    }

    private List<String> splitChunks(String text) {
        if (text == null) {
            return List.of("");
        }
        if (text.length() <= CHUNK_SIZE) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(i + CHUNK_SIZE, text.length())));
        }
        return chunks;
    }

    private Embedding embed(String text) {
        return embedCache.get(text, t -> embeddingModel.embed(t).content());
    }

    // ===================== 检索 =====================

    /**
     * 语义检索（带分数阈值），返回最相似的 topK 个文本片段。
     *
     * @param query    查询文本
     * @param topK     返回数量
     * @param minScore 最小相似度阈值（COSINE 下越高越相关，<=0 表示不过滤）
     */
    public List<TextSegment> search(String query, int topK, double minScore) {
        return searchWithScore(query, topK, minScore).stream()
                .map(EmbeddingMatch::embedded)
                .toList();
    }

    /**
     * 语义检索，不过滤分数。
     */
    public List<TextSegment> search(String query, int topK) {
        return search(query, topK, 0.0);
    }

    /**
     * 类型过滤检索：仅返回 metadata.type == type 的片段（结合分数阈值）。
     */
    public List<TextSegment> searchByType(String query, int topK, double minScore, String type) {
        return searchWithScore(query, topK, minScore).stream()
                .filter(m -> type == null || type.equals(m.embedded().metadata().getString("type")))
                .map(EmbeddingMatch::embedded)
                .toList();
    }

    /**
     * 语义检索（带分数），返回含相似度分数的匹配结果。
     */
    public List<EmbeddingMatch<TextSegment>> searchWithScore(String query, int topK, double minScore) {
        Embedding queryEmbedding = embed(query);
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(topK)
                        .build());
        List<EmbeddingMatch<TextSegment>> matches = result.matches();
        if (minScore > 0) {
            matches = matches.stream()
                    .filter(m -> m.score() >= minScore)
                    .toList();
        }
        return matches;
    }

    /**
     * 供 AiServices 检索增强（RAG）使用的内容检索器。
     *
     * @param topK     召回数量
     * @param minScore 最小相似度阈值
     */
    public EmbeddingStoreContentRetriever toContentRetriever(int topK, double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(topK)
                .minScore(minScore)
                .build();
    }
}
