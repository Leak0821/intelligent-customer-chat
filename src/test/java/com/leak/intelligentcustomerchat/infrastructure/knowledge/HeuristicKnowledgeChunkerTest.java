package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicKnowledgeChunkerTest {
    private final HeuristicKnowledgeChunker chunker = new HeuristicKnowledgeChunker();

    @Test
    void shouldSplitParagraphsIntoChunks() {
        KnowledgeDocument document = new KnowledgeDocument(
                "doc-1",
                "Policy",
                "Paragraph one.\n\nParagraph two.\n\nParagraph three.",
                Map.of("scene", "AFTER_SALES")
        );

        assertThat(chunker.chunk(document)).hasSize(3);
        assertThat(chunker.chunk(document)).allMatch(chunk -> chunk.parentDocumentId().equals("doc-1"));
    }

    @Test
    void shouldSplitLongParagraphBySentenceBoundary() {
        KnowledgeDocument document = new KnowledgeDocument(
                "doc-2",
                "Long Policy",
                ("Sentence one explains the return policy in detail. ".repeat(8)
                        + "Sentence two keeps adding more operational guidance. ".repeat(8)).trim(),
                Map.of("scene", "AFTER_SALES")
        );

        assertThat(chunker.chunk(document)).hasSizeGreaterThan(1);
        assertThat(chunker.chunk(document)).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(680));
    }
}
