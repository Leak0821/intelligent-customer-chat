package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.Map;
import java.util.Objects;

public record KnowledgeDocument(
        String documentId,
        String title,
        String content,
        Map<String, String> metadata
) {
    public KnowledgeDocument {
        Objects.requireNonNull(documentId, "documentId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
