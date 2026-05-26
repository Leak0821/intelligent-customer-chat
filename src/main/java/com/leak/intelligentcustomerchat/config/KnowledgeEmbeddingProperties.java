package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.knowledge.embedding")
public record KnowledgeEmbeddingProperties(
        boolean enabled,
        @Min(1) int maxInputLength
) {
}
