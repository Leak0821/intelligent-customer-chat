package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ChunkIndexWriter;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ElasticsearchKnowledgeIndexManager;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeChunker;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeRetriever;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeSeedCatalog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeAdminController {
    private final KnowledgeRetriever knowledgeRetriever;
    private final ChunkIndexWriter chunkIndexWriter;
    private final ObjectProvider<ElasticsearchKnowledgeIndexManager> indexManagerProvider;
    private final KnowledgeSeedCatalog knowledgeSeedCatalog;
    private final KnowledgeChunker knowledgeChunker;

    public KnowledgeAdminController(KnowledgeRetriever knowledgeRetriever,
                                    ChunkIndexWriter chunkIndexWriter,
                                    ObjectProvider<ElasticsearchKnowledgeIndexManager> indexManagerProvider,
                                    KnowledgeSeedCatalog knowledgeSeedCatalog,
                                    KnowledgeChunker knowledgeChunker) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.chunkIndexWriter = chunkIndexWriter;
        this.indexManagerProvider = indexManagerProvider;
        this.knowledgeSeedCatalog = knowledgeSeedCatalog;
        this.knowledgeChunker = knowledgeChunker;
    }

    @GetMapping("/index/status")
    public IndexStatusResult indexStatus() {
        ElasticsearchKnowledgeIndexManager indexManager = indexManagerProvider.getIfAvailable();
        if (indexManager == null) {
            return new IndexStatusResult("", false, false);
        }
        ElasticsearchKnowledgeIndexManager.KnowledgeIndexStatus status = indexManager.status();
        return new IndexStatusResult(status.indexName(), status.elasticsearchEnabled(), status.exists());
    }

    @PostMapping("/index/ensure")
    public IndexStatusResult ensureIndex() {
        ElasticsearchKnowledgeIndexManager indexManager = indexManagerProvider.getIfAvailable();
        if (indexManager == null) {
            return new IndexStatusResult("", false, false);
        }
        ElasticsearchKnowledgeIndexManager.KnowledgeIndexStatus status = indexManager.ensureIndex();
        return new IndexStatusResult(status.indexName(), status.elasticsearchEnabled(), status.exists());
    }

    @GetMapping("/seeds")
    public List<KnowledgeSeedPreview> listSeeds() {
        return knowledgeSeedCatalog.builtInDocuments().stream()
                .map(document -> new KnowledgeSeedPreview(
                        document.documentId(),
                        document.title(),
                        document.metadata().getOrDefault("scene", "UNKNOWN"),
                        document.metadata().getOrDefault("subIntents", "general_inquiry"),
                        knowledgeChunker.chunk(document).size()
                ))
                .toList();
    }

    @PostMapping("/index/seeds")
    public SeedIndexResult indexSeeds() {
        List<KnowledgeDocument> documents = knowledgeSeedCatalog.builtInDocuments();
        int indexedDocuments = 0;
        int indexedRecords = 0;
        for (KnowledgeDocument document : documents) {
            indexedDocuments++;
            indexedRecords += chunkIndexWriter.index(document);
        }
        return new SeedIndexResult(indexedDocuments, indexedRecords);
    }

    @PostMapping("/index/sample")
    public IndexResult indexSample(@RequestBody SampleKnowledgeRequest request) {
        KnowledgeDocument document = new KnowledgeDocument(
                request.documentId() == null || request.documentId().isBlank() ? UUID.randomUUID().toString() : request.documentId(),
                request.title(),
                request.content(),
                request.metadata()
        );
        int indexed = chunkIndexWriter.index(document);
        return new IndexResult(document.documentId(), indexed);
    }

    @GetMapping("/search")
    public KnowledgeRetrieveResult search(@RequestParam String q,
                                          @RequestParam(defaultValue = "UNKNOWN") String scene,
                                          @RequestParam(defaultValue = "general_inquiry") String subIntent,
                                          @RequestParam(defaultValue = "10") int topK) {
        return knowledgeRetriever.retrieve(new RetrievalQuery(q, scene, subIntent, List.of(), topK));
    }

    public record SampleKnowledgeRequest(
            String documentId,
            String title,
            String content,
            Map<String, String> metadata
    ) {
    }

    public record IndexResult(
            String documentId,
            int indexedDocumentCount
    ) {
    }

    public record IndexStatusResult(
            String indexName,
            boolean elasticsearchEnabled,
            boolean exists
    ) {
    }

    public record KnowledgeSeedPreview(
            String documentId,
            String title,
            String scene,
            String subIntents,
            int chunkCount
    ) {
    }

    public record SeedIndexResult(
            int indexedDocumentCount,
            int indexedRecordCount
    ) {
    }
}
