package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ChunkIndexWriter;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.HeuristicKnowledgeChunker;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeContentHasher;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeGovernanceStore;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeChunker;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeFileTextExtractor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultKnowledgeImportServiceTest {
    private final KnowledgeChunker chunker = new HeuristicKnowledgeChunker();
    private final RecordingChunkIndexWriter chunkIndexWriter = new RecordingChunkIndexWriter(chunker);
    private final RecordingKnowledgeGovernanceStore governanceStore = new RecordingKnowledgeGovernanceStore();
    private final DefaultKnowledgeImportService service = new DefaultKnowledgeImportService(
            chunkIndexWriter,
            chunker,
            new KnowledgeFileTextExtractor(),
            new KnowledgeContentHasher(),
            governanceStore
    );

    @Test
    void shouldImportCsvFilesIntoParentChildDocuments() {
        KnowledgeImportService.KnowledgeImportBatchResult result = service.importFiles(
                new KnowledgeImportService.KnowledgeImportCommand(
                        List.of(new KnowledgeImportService.KnowledgeImportFile(
                                "catalog.csv",
                                "text/csv",
                                """
                                sku,title,policy
                                A1,Desk Lamp,Ships in 3 days
                                A2,Floor Lamp,30-day return
                                """.getBytes(StandardCharsets.UTF_8)
                        )),
                        "sales-catalog",
                        "2026-02",
                        "active",
                        "PRE_SALES",
                        "product_recommendation",
                        "admin-console"
                )
        );

        assertThat(result.importedFileCount()).isEqualTo(1);
        assertThat(result.parentDocumentCount()).isEqualTo(1);
        assertThat(result.chunkCount()).isEqualTo(2);
        assertThat(result.indexedRecordCount()).isEqualTo(3);
        assertThat(result.documents()).singleElement().satisfies(document -> {
            assertThat(document.title()).isEqualTo("catalog");
            assertThat(document.fileType()).isEqualTo("CSV");
            assertThat(document.scene()).isEqualTo("PRE_SALES");
            assertThat(document.subIntents()).isEqualTo("product_recommendation");
            assertThat(document.chunkCount()).isEqualTo(2);
            assertThat(document.metadata()).containsEntry("source", "admin-console");
            assertThat(document.metadata()).containsEntry("sourceFileName", "catalog.csv");
            assertThat(document.metadata()).containsEntry("fileType", "CSV");
            assertThat(document.metadata()).containsEntry("knowledge_key", "sales-catalog");
            assertThat(document.metadata()).containsEntry("version", "2026-02");
            assertThat(document.metadata()).containsEntry("status", "active");
            assertThat(document.metadata()).containsKey("content_hash");
        });
        assertThat(chunkIndexWriter.indexedDocuments).singleElement().satisfies(document -> {
            assertThat(document.content()).contains("记录 1");
            assertThat(document.content()).contains("sku: A1");
            assertThat(document.content()).contains("policy: 30-day return");
        });
        assertThat(governanceStore.validatedKeys).containsExactly("sales-catalog::2026-02");
        assertThat(governanceStore.deprecatedKeys).containsExactly("sales-catalog::2026-02");
    }

    @Test
    void shouldRejectEmptyFileBatch() {
        assertThatThrownBy(() -> service.importFiles(
                new KnowledgeImportService.KnowledgeImportCommand(List.of(), null, "v1", "active", "PRE_SALES", "general_inquiry", "admin")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少上传一个知识文件");
    }

    @Test
    void shouldRejectDuplicateKnowledgeKeyAndVersionInBatch() {
        assertThatThrownBy(() -> service.importFiles(
                new KnowledgeImportService.KnowledgeImportCommand(
                        List.of(
                                new KnowledgeImportService.KnowledgeImportFile("a.csv", "text/csv", "k,v\n1,2".getBytes(StandardCharsets.UTF_8)),
                                new KnowledgeImportService.KnowledgeImportFile("b.csv", "text/csv", "k,v\n3,4".getBytes(StandardCharsets.UTF_8))
                        ),
                        "same-key",
                        "2026-02",
                        "active",
                        "PRE_SALES",
                        "general_inquiry",
                        "admin"
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能显式指定 knowledge_key");
    }

    private static final class RecordingChunkIndexWriter implements ChunkIndexWriter {
        private final KnowledgeChunker chunker;
        private final List<KnowledgeDocument> indexedDocuments = new ArrayList<>();

        private RecordingChunkIndexWriter(KnowledgeChunker chunker) {
            this.chunker = chunker;
        }

        @Override
        public void ensureIndex() {
        }

        @Override
        public int index(KnowledgeDocument document) {
            indexedDocuments.add(document);
            return chunker.chunk(document).size() + 1;
        }
    }

    private static final class RecordingKnowledgeGovernanceStore implements KnowledgeGovernanceStore {
        private final List<String> validatedKeys = new ArrayList<>();
        private final List<String> deprecatedKeys = new ArrayList<>();

        @Override
        public void validateImport(String knowledgeKey, String version, String contentHash) {
            validatedKeys.add(knowledgeKey + "::" + version);
        }

        @Override
        public int deprecateOlderActiveVersions(String knowledgeKey, String currentVersion) {
            deprecatedKeys.add(knowledgeKey + "::" + currentVersion);
            return 1;
        }
    }
}
