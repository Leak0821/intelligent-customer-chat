package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.List;

public record KnowledgeRetrieveResult(
        String source,
        List<KnowledgeSnippet> snippets,
        int recallCount
) {
    public KnowledgeRetrieveResult {
        source = source == null ? "none" : source;
        snippets = List.copyOf(snippets);
    }

    public static KnowledgeRetrieveResult empty() {
        return new KnowledgeRetrieveResult("none", List.of(), 0);
    }
}
