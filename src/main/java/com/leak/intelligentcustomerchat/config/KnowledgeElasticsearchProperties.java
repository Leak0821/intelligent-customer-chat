package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.knowledge.elasticsearch")
public record KnowledgeElasticsearchProperties(
        boolean enabled,
        @NotBlank String uris,
        @NotBlank String indexName,
        @NotBlank String titleField,
        @NotBlank String contentField,
        @NotBlank String vectorField,
        @NotBlank String parentIdField,
        @NotBlank String docTypeField,
        @Min(1) int topK,
        @Min(1) int numCandidates,
        @Min(4) int embeddingDimensions,
        @Min(10) int rrfK
) {
}
