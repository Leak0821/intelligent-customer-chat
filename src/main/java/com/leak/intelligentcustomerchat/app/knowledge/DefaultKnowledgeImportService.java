package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.ChunkIndexWriter;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeContentHasher;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeGovernanceStore;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeChunker;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeFileTextExtractor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DefaultKnowledgeImportService implements KnowledgeImportService {
    private final ChunkIndexWriter chunkIndexWriter;
    private final KnowledgeChunker knowledgeChunker;
    private final KnowledgeFileTextExtractor knowledgeFileTextExtractor;
    private final KnowledgeContentHasher knowledgeContentHasher;
    private final KnowledgeGovernanceStore knowledgeGovernanceStore;

    public DefaultKnowledgeImportService(ChunkIndexWriter chunkIndexWriter,
                                         KnowledgeChunker knowledgeChunker,
                                         KnowledgeFileTextExtractor knowledgeFileTextExtractor,
                                         KnowledgeContentHasher knowledgeContentHasher,
                                         KnowledgeGovernanceStore knowledgeGovernanceStore) {
        this.chunkIndexWriter = chunkIndexWriter;
        this.knowledgeChunker = knowledgeChunker;
        this.knowledgeFileTextExtractor = knowledgeFileTextExtractor;
        this.knowledgeContentHasher = knowledgeContentHasher;
        this.knowledgeGovernanceStore = knowledgeGovernanceStore;
    }

    @Override
    public KnowledgeImportBatchResult importFiles(KnowledgeImportCommand command) {
        if (command.files().isEmpty()) {
            throw new IllegalArgumentException("至少上传一个知识文件");
        }
        if (command.files().size() > 1 && command.knowledgeKey() != null) {
            throw new IllegalArgumentException("批量上传多个文件时不能显式指定 knowledge_key，请让系统按文件名自动生成");
        }
        validateStatus(command.status());

        String batchId = UUID.randomUUID().toString();
        String importedAt = OffsetDateTime.now().toString();
        chunkIndexWriter.ensureIndex();

        List<KnowledgeImportDocumentResult> documents = new ArrayList<>();
        int totalChunks = 0;
        int totalIndexedRecords = 0;
        int documentSequence = 1;
        Set<String> batchVersionKeys = new HashSet<>();
        Set<String> batchContentKeys = new HashSet<>();
        for (KnowledgeImportFile file : command.files()) {
            KnowledgeFileTextExtractor.ExtractedKnowledgeFile extracted = knowledgeFileTextExtractor.extract(file);
            String knowledgeKey = resolveKnowledgeKey(command, file, extracted, batchId, documentSequence);
            String contentHash = knowledgeContentHasher.hashText(extracted.content());
            ensureBatchUniqueness(batchVersionKeys, batchContentKeys, knowledgeKey, command.version(), contentHash);
            knowledgeGovernanceStore.validateImport(knowledgeKey, command.version(), contentHash);
            Map<String, String> metadata = buildMetadata(command, file, extracted, batchId, importedAt, knowledgeKey, contentHash);
            String documentId = batchId + "-" + documentSequence++;
            KnowledgeDocument document = new KnowledgeDocument(documentId, extracted.title(), extracted.content(), metadata);
            int chunkCount = knowledgeChunker.chunk(document).size();
            int indexedRecordCount = chunkIndexWriter.index(document);
            if ("active".equals(command.status())) {
                knowledgeGovernanceStore.deprecateOlderActiveVersions(knowledgeKey, command.version());
            }
            totalChunks += chunkCount;
            totalIndexedRecords += indexedRecordCount;
            documents.add(new KnowledgeImportDocumentResult(
                    documentId,
                    document.title(),
                    file.filename(),
                    extracted.fileType(),
                    command.scene(),
                    command.subIntents(),
                    chunkCount,
                    indexedRecordCount,
                    metadata
            ));
        }

        return new KnowledgeImportBatchResult(
                batchId,
                documents.size(),
                documents.size(),
                totalChunks,
                totalIndexedRecords,
                documents
        );
    }

    private Map<String, String> buildMetadata(KnowledgeImportCommand command,
                                              KnowledgeImportFile file,
                                              KnowledgeFileTextExtractor.ExtractedKnowledgeFile extracted,
                                              String batchId,
                                              String importedAt,
                                              String knowledgeKey,
                                              String contentHash) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("knowledge_key", knowledgeKey);
        metadata.put("version", command.version());
        metadata.put("status", command.status());
        metadata.put("content_hash", contentHash);
        metadata.put("scene", command.scene());
        metadata.put("subIntents", command.subIntents());
        metadata.put("source", command.source());
        metadata.put("sourceFileName", file.filename());
        metadata.put("sourceContentType", file.contentType());
        metadata.put("fileType", extracted.fileType());
        metadata.put("importBatchId", batchId);
        metadata.put("importedAt", importedAt);
        return metadata;
    }

    private void validateStatus(String status) {
        if (!"active".equals(status) && !"deprecated".equals(status)) {
            throw new IllegalArgumentException("知识状态仅支持 active 或 deprecated");
        }
    }

    private String resolveKnowledgeKey(KnowledgeImportCommand command,
                                       KnowledgeImportFile file,
                                       KnowledgeFileTextExtractor.ExtractedKnowledgeFile extracted,
                                       String batchId,
                                       int documentSequence) {
        if (command.knowledgeKey() != null) {
            return command.knowledgeKey();
        }
        String base = (extracted.title() == null || extracted.title().isBlank()) ? file.filename() : extracted.title();
        String slug = base == null ? "" : base.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!slug.isBlank()) {
            return slug;
        }
        return "knowledge-" + batchId.substring(0, 8) + "-" + documentSequence;
    }

    private void ensureBatchUniqueness(Set<String> batchVersionKeys,
                                       Set<String> batchContentKeys,
                                       String knowledgeKey,
                                       String version,
                                       String contentHash) {
        String versionKey = knowledgeKey + "::" + version;
        if (!batchVersionKeys.add(versionKey)) {
            throw new IllegalArgumentException("当前批次内存在重复的 knowledge_key 和 version: " + knowledgeKey + " / " + version);
        }
        String contentKey = knowledgeKey + "::" + contentHash;
        if (!batchContentKeys.add(contentKey)) {
            throw new IllegalArgumentException("当前批次内存在重复的知识内容: " + knowledgeKey);
        }
    }
}
