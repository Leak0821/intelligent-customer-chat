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
}
