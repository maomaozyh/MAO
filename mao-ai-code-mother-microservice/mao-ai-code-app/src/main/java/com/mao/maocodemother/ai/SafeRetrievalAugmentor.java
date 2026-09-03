package com.mao.maocodemother.ai;

import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 安全的检索增强装饰器。
 *
 * <p>委托给真实的 {@link RetrievalAugmentor}（基于 Milvus 向量检索）做 RAG，
 * 但当向量库 / Embedding 服务不可用时捕获异常、退回原始消息（不附加检索内容），
 * 保证检索增强是"尽力而为"，不会因基础设施异常而中断代码生成主流程。
 */
@Slf4j
public class SafeRetrievalAugmentor implements RetrievalAugmentor {

    private final RetrievalAugmentor delegate;

    public SafeRetrievalAugmentor(RetrievalAugmentor delegate) {
        this.delegate = delegate;
    }

    @Override
    public AugmentationResult augment(AugmentationRequest request) {
        try {
            return delegate.augment(request);
        } catch (Exception e) {
            log.warn("RAG 检索失败，跳过检索增强（返回原始消息）: {}", e.getMessage());
            return new AugmentationResult(request.chatMessage(), List.of());
        }
    }
}
