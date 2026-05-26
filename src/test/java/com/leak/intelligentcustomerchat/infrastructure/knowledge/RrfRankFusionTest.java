package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RrfRankFusionTest {
    private final RrfRankFusion fusion = new RrfRankFusion();

    @Test
    void shouldBoostDocumentsAppearingInBothLists() {
        KnowledgeSnippet shared = new KnowledgeSnippet("shared", "Shared", "shared content", 1.0d, "test");
        KnowledgeSnippet bm25Only = new KnowledgeSnippet("bm25", "Bm25", "bm25 content", 0.8d, "test");
        KnowledgeSnippet vectorOnly = new KnowledgeSnippet("vector", "Vector", "vector content", 0.8d, "test");

        List<KnowledgeSnippet> fused = fusion.fuse(
                List.of(shared, bm25Only),
                List.of(shared, vectorOnly),
                60,
                3
        );

        assertThat(fused).hasSize(3);
        assertThat(fused.get(0).id()).isEqualTo("shared");
    }
}
