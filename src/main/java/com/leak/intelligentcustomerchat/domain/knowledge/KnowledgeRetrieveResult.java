package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.List;

public record KnowledgeRetrieveResult(
        List<String> snippets,
        int recallCount
) {
    public KnowledgeRetrieveResult {
        snippets = List.copyOf(snippets);
    }

    public static KnowledgeRetrieveResult empty() {
        return new KnowledgeRetrieveResult(List.of(), 0);
    }
}
