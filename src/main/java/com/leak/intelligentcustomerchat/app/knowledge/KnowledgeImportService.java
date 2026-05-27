package com.leak.intelligentcustomerchat.app.knowledge;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface KnowledgeImportService {
    KnowledgeImportBatchResult importFiles(KnowledgeImportCommand command);

    record KnowledgeImportCommand(
            List<KnowledgeImportFile> files,
            String knowledgeKey,
            String version,
            String status,
            String scene,
            String subIntents,
            String source
    ) {
        public KnowledgeImportCommand {
            files = files == null ? List.of() : List.copyOf(files);
            knowledgeKey = normalizeNullable(knowledgeKey);
            version = normalize(version, "v1");
            status = normalize(status, "active").toLowerCase();
            scene = normalize(scene, "UNKNOWN");
            subIntents = normalize(subIntents, "general_inquiry");
            source = normalize(source, "admin-upload");
        }
    }

    record KnowledgeImportFile(
            String filename,
            String contentType,
            byte[] content
    ) {
        public KnowledgeImportFile {
            filename = normalize(filename, "untitled");
            contentType = normalize(contentType, "application/octet-stream");
            content = content == null ? new byte[0] : content.clone();
        }
    }

    record KnowledgeImportBatchResult(
            String batchId,
            int importedFileCount,
            int parentDocumentCount,
            int chunkCount,
            int indexedRecordCount,
            List<KnowledgeImportDocumentResult> documents
    ) {
        public KnowledgeImportBatchResult {
            Objects.requireNonNull(batchId, "batchId must not be null");
            documents = documents == null ? List.of() : List.copyOf(documents);
        }
    }

    record KnowledgeImportDocumentResult(
            String documentId,
            String title,
            String originalFilename,
            String fileType,
            String scene,
            String subIntents,
            int chunkCount,
            int indexedRecordCount,
            Map<String, String> metadata
    ) {
        public KnowledgeImportDocumentResult {
            Objects.requireNonNull(documentId, "documentId must not be null");
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(originalFilename, "originalFilename must not be null");
            Objects.requireNonNull(fileType, "fileType must not be null");
            scene = normalize(scene, "UNKNOWN");
            subIntents = normalize(subIntents, "general_inquiry");
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
