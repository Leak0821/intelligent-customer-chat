package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RrfRankFusion {

    public List<KnowledgeSnippet> fuse(List<KnowledgeSnippet> bm25Snippets,
                                       List<KnowledgeSnippet> vectorSnippets,
                                       int rrfK,
                                       int topK) {
        Map<String, ScoredSnippet> scoreMap = new LinkedHashMap<>();
        accumulate(scoreMap, bm25Snippets, rrfK);
        accumulate(scoreMap, vectorSnippets, rrfK);
        return scoreMap.values().stream()
                .sorted(Comparator.comparingDouble(ScoredSnippet::score).reversed())
                .limit(topK)
                .map(ScoredSnippet::snippet)
                .toList();
    }

    private void accumulate(Map<String, ScoredSnippet> scoreMap, List<KnowledgeSnippet> snippets, int rrfK) {
        for (int index = 0; index < snippets.size(); index++) {
            KnowledgeSnippet snippet = snippets.get(index);
            double score = 1.0d / (rrfK + index + 1);
            scoreMap.compute(snippet.id(), (key, existing) -> existing == null
                    ? new ScoredSnippet(snippet, score)
                    : new ScoredSnippet(existing.snippet(), existing.score() + score));
        }
    }

    private record ScoredSnippet(KnowledgeSnippet snippet, double score) {
    }
}
