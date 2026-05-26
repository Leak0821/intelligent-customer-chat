package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.Objects;

public record KnowledgeSnippet(
        String id,
        String title,
        String content,
        double score,
        String source
) {
    public KnowledgeSnippet {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}
