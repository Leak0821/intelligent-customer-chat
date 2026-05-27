package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeImportService;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ChunkIndexWriter;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ElasticsearchKnowledgeIndexManager;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeChunker;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeRetriever;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeSeedCatalog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final KnowledgeImportService knowledgeImportService;

    public KnowledgeAdminController(KnowledgeRetriever knowledgeRetriever,
                                    ChunkIndexWriter chunkIndexWriter,
                                    ObjectProvider<ElasticsearchKnowledgeIndexManager> indexManagerProvider,
                                    KnowledgeSeedCatalog knowledgeSeedCatalog,
                                    KnowledgeChunker knowledgeChunker,
                                    KnowledgeImportService knowledgeImportService) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.chunkIndexWriter = chunkIndexWriter;
        this.indexManagerProvider = indexManagerProvider;
        this.knowledgeSeedCatalog = knowledgeSeedCatalog;
        this.knowledgeChunker = knowledgeChunker;
        this.knowledgeImportService = knowledgeImportService;
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
        String documentId = request.documentId() == null || request.documentId().isBlank() ? UUID.randomUUID().toString() : request.documentId();
        Map<String, String> metadata = new java.util.LinkedHashMap<>(request.metadata() == null ? Map.of() : request.metadata());
        metadata.putIfAbsent("knowledge_key", documentId);
        metadata.putIfAbsent("version", "manual-v1");
        metadata.putIfAbsent("status", "active");
        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                request.title(),
                request.content(),
                metadata
        );
        int indexed = chunkIndexWriter.index(document);
        return new IndexResult(document.documentId(), indexed);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeImportService.KnowledgeImportBatchResult importFiles(@RequestParam("files") MultipartFile[] files,
                                                                         @RequestParam(required = false) String knowledgeKey,
                                                                         @RequestParam(defaultValue = "v1") String version,
                                                                         @RequestParam(defaultValue = "active") String status,
                                                                         @RequestParam(defaultValue = "UNKNOWN") String scene,
                                                                         @RequestParam(defaultValue = "general_inquiry") String subIntents,
                                                                         @RequestParam(defaultValue = "admin-upload") String source) {
        List<KnowledgeImportService.KnowledgeImportFile> importFiles = List.of(files).stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(this::toImportFile)
                .toList();
        return knowledgeImportService.importFiles(
                new KnowledgeImportService.KnowledgeImportCommand(importFiles, knowledgeKey, version, status, scene, subIntents, source)
        );
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

    private KnowledgeImportService.KnowledgeImportFile toImportFile(MultipartFile file) {
        try {
            return new KnowledgeImportService.KnowledgeImportFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取上传文件失败: " + file.getOriginalFilename(), ex);
        }
    }
}
