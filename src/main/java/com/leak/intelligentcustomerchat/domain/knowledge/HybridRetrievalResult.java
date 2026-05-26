package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.List;

public record HybridRetrievalResult(
        List<KnowledgeSnippet> snippets,
        List<KnowledgeSnippet> bm25Snippets,
        List<KnowledgeSnippet> vectorSnippets
) {
    public HybridRetrievalResult {
        snippets = List.copyOf(snippets);
        bm25Snippets = List.copyOf(bm25Snippets);
        vectorSnippets = List.copyOf(vectorSnippets);
    }
}
