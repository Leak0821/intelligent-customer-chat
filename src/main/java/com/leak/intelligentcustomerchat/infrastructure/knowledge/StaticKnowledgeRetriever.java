package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeChunk;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StaticKnowledgeRetriever implements KnowledgeRetriever {
    private final KnowledgeSeedCatalog knowledgeSeedCatalog;
    private final KnowledgeChunker knowledgeChunker;

    public StaticKnowledgeRetriever(KnowledgeSeedCatalog knowledgeSeedCatalog, KnowledgeChunker knowledgeChunker) {
        this.knowledgeSeedCatalog = knowledgeSeedCatalog;
        this.knowledgeChunker = knowledgeChunker;
    }

    @Override
    public KnowledgeRetrieveResult retrieve(RetrievalQuery query) {
        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (KnowledgeDocument document : knowledgeSeedCatalog.builtInDocuments()) {
            for (KnowledgeChunk chunk : knowledgeChunker.chunk(document)) {
                double score = scoreChunk(query, chunk);
                if (score > 0.0d) {
                    scoredChunks.add(new ScoredChunk(chunk, score));
                }
            }
        }

        if (scoredChunks.isEmpty()) {
            KnowledgeDocument fallbackDocument = knowledgeSeedCatalog.builtInDocuments().get(0);
            KnowledgeChunk fallbackChunk = knowledgeChunker.chunk(fallbackDocument).get(0);
            return new KnowledgeRetrieveResult(
                    "static-knowledge-retriever",
                    List.of(toSnippet(fallbackChunk, 0.1d)),
                    1
            );
        }

        List<KnowledgeSnippet> snippets = scoredChunks.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, query.topK()))
                .map(scoredChunk -> toSnippet(scoredChunk.chunk(), scoredChunk.score()))
                .toList();
        return new KnowledgeRetrieveResult("static-knowledge-retriever", snippets, snippets.size());
    }

    private double scoreChunk(RetrievalQuery query, KnowledgeChunk chunk) {
        double score = 0.0d;
        String normalizedChunkText = (chunk.title() + " " + chunk.content()).toLowerCase(Locale.ROOT);
        Set<String> queryTerms = tokenize(query.queryText() + " " + query.subIntent());
        for (String term : queryTerms) {
            if (normalizedChunkText.contains(term)) {
                score += 1.0d;
            }
        }
        if (matchesMetadata(chunk.metadata().get("scene"), query.scene())) {
            score += 3.0d;
        }
        if (matchesAnySubIntent(chunk.metadata().get("subIntents"), query.subIntent())) {
            score += 4.0d;
        }
        return score;
    }

    private boolean matchesMetadata(String metadataValue, String expectedValue) {
        if (metadataValue == null || expectedValue == null) {
            return false;
        }
        return metadataValue.equalsIgnoreCase(expectedValue);
    }

    private boolean matchesAnySubIntent(String subIntents, String expectedIntent) {
        if (subIntents == null || expectedIntent == null) {
            return false;
        }
        for (String value : subIntents.split(",")) {
            if (value.trim().equalsIgnoreCase(expectedIntent)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+")) {
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private KnowledgeSnippet toSnippet(KnowledgeChunk chunk, double score) {
        return new KnowledgeSnippet(
                UUID.randomUUID().toString(),
                chunk.title(),
                chunk.content(),
                score,
                chunk.metadata().getOrDefault("source", "static-knowledge-retriever")
        );
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
