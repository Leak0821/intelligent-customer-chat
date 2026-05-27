package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeChunk;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchChunkIndexWriter implements ChunkIndexWriter {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KnowledgeElasticsearchProperties properties;
    private final KnowledgeChunker knowledgeChunker;
    private final EmbeddingService embeddingService;
    private final ElasticsearchKnowledgeIndexManager indexManager;
    private final KnowledgeContentHasher knowledgeContentHasher;

    public ElasticsearchChunkIndexWriter(RestClient restClient,
                                         ObjectMapper objectMapper,
                                         KnowledgeElasticsearchProperties properties,
                                         KnowledgeChunker knowledgeChunker,
                                         EmbeddingService embeddingService,
                                         ElasticsearchKnowledgeIndexManager indexManager,
                                         KnowledgeContentHasher knowledgeContentHasher) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.knowledgeChunker = knowledgeChunker;
        this.embeddingService = embeddingService;
        this.indexManager = indexManager;
        this.knowledgeContentHasher = knowledgeContentHasher;
    }

    @Override
    public void ensureIndex() {
        indexManager.ensureIndex();
    }

    @Override
    public int index(KnowledgeDocument document) {
        ensureIndex();
        int indexed = 0;
        indexed += upsertDocument(buildParentDocumentId(document), buildParentBody(document));
        List<KnowledgeChunk> chunks = knowledgeChunker.chunk(document);
        for (KnowledgeChunk chunk : chunks) {
            indexed += upsertDocument(buildChunkDocumentId(chunk), buildChunkBody(chunk));
        }
        return indexed;
    }

    private int upsertDocument(String id, Map<String, Object> body) {
        try {
            Request request = new Request("PUT", "/" + properties.indexName() + "/_doc/" + id);
            request.setJsonEntity(objectMapper.writeValueAsString(body));
            restClient.performRequest(request);
            return 1;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to write knowledge document to elasticsearch", ex);
        }
    }

    private String buildParentDocumentId(KnowledgeDocument document) {
        return "parent-" + document.documentId();
    }

    private Map<String, Object> buildParentBody(KnowledgeDocument document) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(properties.docTypeField(), "parent");
        body.put(properties.parentIdField(), document.documentId());
        body.put(properties.titleField(), document.title());
        body.put(properties.contentField(), document.content());
        body.put(properties.vectorField(), embeddingService.embed(document.content()));
        body.put("content_hash", knowledgeContentHasher.hashText(document.content()));
        body.putAll(document.metadata());
        return body;
    }

    private Map<String, Object> buildChunkBody(KnowledgeChunk chunk) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(properties.docTypeField(), "chunk");
        body.put(properties.parentIdField(), chunk.parentDocumentId());
        body.put(properties.titleField(), chunk.title());
        body.put(properties.contentField(), chunk.content());
        body.put(properties.vectorField(), embeddingService.embed(chunk.content()));
        body.put("chunk_order", chunk.chunkOrder());
        body.put("chunk_hash", knowledgeContentHasher.hashText(chunk.content()));
        body.putAll(chunk.metadata());
        return body;
    }

    private String buildChunkDocumentId(KnowledgeChunk chunk) {
        return "chunk-" + chunk.parentDocumentId() + "-" + chunk.chunkOrder() + "-" + knowledgeContentHasher.shortHash(chunk.content());
    }
}
