package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.Map;
import java.util.Objects;

public record KnowledgeChunk(
        String chunkId,
        String parentDocumentId,
        String title,
        String content,
        int chunkOrder,
        Map<String, String> metadata
) {
    public KnowledgeChunk {
        Objects.requireNonNull(chunkId, "chunkId must not be null");
        Objects.requireNonNull(parentDocumentId, "parentDocumentId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
