package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.knowledge.elasticsearch")
public record KnowledgeElasticsearchProperties(
        @NotBlank String uris,
        @NotBlank String indexName,
        @Min(1) int topK
) {
}
