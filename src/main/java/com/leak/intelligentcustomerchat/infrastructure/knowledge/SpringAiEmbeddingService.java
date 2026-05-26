package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.config.KnowledgeEmbeddingProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnProperty(prefix = "app.knowledge.embedding", name = "enabled", havingValue = "true")
public class SpringAiEmbeddingService implements EmbeddingService {
    private final EmbeddingModel embeddingModel;
    private final KnowledgeEmbeddingProperties properties;

    public SpringAiEmbeddingService(EmbeddingModel embeddingModel, KnowledgeEmbeddingProperties properties) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    @Override
    public float[] embed(String text) {
        String normalized = text == null ? "" : text;
        if (normalized.length() > properties.maxInputLength()) {
            normalized = normalized.substring(0, properties.maxInputLength());
        }
        List<float[]> embeddings = embeddingModel.embed(List.of(normalized));
        return embeddings.isEmpty() ? new float[0] : embeddings.get(0);
    }

    @Override
    public int dimensions() {
        float[] probe = embed("embedding dimension probe");
        return probe.length;
    }
}
