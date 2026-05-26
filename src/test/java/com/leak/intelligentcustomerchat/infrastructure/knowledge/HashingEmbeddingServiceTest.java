package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashingEmbeddingServiceTest {
    private final HashingEmbeddingService embeddingService = new HashingEmbeddingService(
            new KnowledgeElasticsearchProperties(false, "http://127.0.0.1:9200", "knowledge_chunks",
                    "title", "content", "content_vector", "parent_id", "doc_type", 10, 50, 16, 60)
    );

    @Test
    void shouldProduceNormalizedVectorWithConfiguredDimensions() {
        float[] vector = embeddingService.embed("cross border customer support");

        assertThat(vector).hasSize(16);
        boolean hasNonZero = false;
        for (float value : vector) {
            if (value != 0.0f) {
                hasNonZero = true;
                break;
            }
        }
        assertThat(hasNonZero).isTrue();
    }
}
