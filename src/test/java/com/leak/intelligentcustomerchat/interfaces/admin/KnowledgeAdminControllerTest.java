package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ChunkIndexWriter;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.HeuristicKnowledgeChunker;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeRetriever;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeSeedCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAdminControllerTest {

    @Test
    void shouldListBuiltInSeedsAndBatchIndexThem() {
        RecordingChunkIndexWriter chunkIndexWriter = new RecordingChunkIndexWriter();
        KnowledgeAdminController controller = new KnowledgeAdminController(
                query -> new KnowledgeRetrieveResult("stub", List.of(new KnowledgeSnippet("1", "title", "content", 1.0d, "stub")), 1),
                chunkIndexWriter,
                new EmptyObjectProvider<>(),
                new KnowledgeSeedCatalog(),
                new HeuristicKnowledgeChunker()
        );

        List<KnowledgeAdminController.KnowledgeSeedPreview> previews = controller.listSeeds();
        KnowledgeAdminController.SeedIndexResult result = controller.indexSeeds();

        assertThat(previews).isNotEmpty();
        assertThat(previews).anySatisfy(preview -> assertThat(preview.scene()).isIn("PRE_SALES", "AFTER_SALES"));
        assertThat(result.indexedDocumentCount()).isEqualTo(previews.size());
        assertThat(result.indexedRecordCount()).isGreaterThan(result.indexedDocumentCount());
        assertThat(chunkIndexWriter.indexedDocuments).hasSize(previews.size());
    }

    private static final class RecordingChunkIndexWriter implements ChunkIndexWriter {
        private final List<KnowledgeDocument> indexedDocuments = new ArrayList<>();

        @Override
        public void ensureIndex() {
        }

        @Override
        public int index(KnowledgeDocument document) {
            indexedDocuments.add(document);
            return Math.max(1, document.content().split("\\n\\s*\\n").length + 1);
        }
    }

    private static final class EmptyObjectProvider<T> implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T getIfAvailable() {
            return null;
        }

        @Override
        public T getIfUnique() {
            return null;
        }

        @Override
        public T getObject() {
            throw new UnsupportedOperationException();
        }
    }
}
