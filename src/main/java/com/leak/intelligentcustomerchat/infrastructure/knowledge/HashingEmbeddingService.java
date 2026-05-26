package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EmbeddingService.class)
public class HashingEmbeddingService implements EmbeddingService {
    private final KnowledgeElasticsearchProperties properties;

    public HashingEmbeddingService(KnowledgeElasticsearchProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[properties.embeddingDimensions()];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (int index = 0; index < text.length(); index++) {
            int slot = index % vector.length;
            vector[slot] += (float) text.charAt(index);
        }
        normalize(vector);
        return vector;
    }

    @Override
    public int dimensions() {
        return properties.embeddingDimensions();
    }

    private void normalize(float[] vector) {
        double magnitude = 0.0d;
        for (float value : vector) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0.0d) {
            return;
        }
        for (int index = 0; index < vector.length; index++) {
            vector[index] = (float) (vector[index] / magnitude);
        }
    }
}
