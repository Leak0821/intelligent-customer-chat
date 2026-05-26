package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeChunk;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
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

    public ElasticsearchChunkIndexWriter(RestClient restClient,
                                         ObjectMapper objectMapper,
                                         KnowledgeElasticsearchProperties properties,
                                         KnowledgeChunker knowledgeChunker,
                                         EmbeddingService embeddingService) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.knowledgeChunker = knowledgeChunker;
        this.embeddingService = embeddingService;
    }

    @Override
    public void ensureIndex() {
        try {
            Request request = new Request("PUT", "/" + properties.indexName());
            request.setJsonEntity(buildIndexMapping());
            restClient.performRequest(request);
        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() != 400) {
                throw new IllegalStateException("failed to ensure elasticsearch index", ex);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("failed to ensure elasticsearch index", ex);
        }
    }

    @Override
    public int index(KnowledgeDocument document) {
        ensureIndex();
        int indexed = 0;
        indexed += upsertDocument(buildParentDocumentId(document), buildParentBody(document));
        List<KnowledgeChunk> chunks = knowledgeChunker.chunk(document);
        for (KnowledgeChunk chunk : chunks) {
            indexed += upsertDocument(chunk.chunkId(), buildChunkBody(chunk));
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

    private String buildIndexMapping() throws IOException {
        Map<String, Object> mapping = new LinkedHashMap<>();
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put(properties.docTypeField(), Map.of("type", "keyword"));
        propertiesMap.put(properties.parentIdField(), Map.of("type", "keyword"));
        propertiesMap.put(properties.titleField(), Map.of("type", "text"));
        propertiesMap.put(properties.contentField(), Map.of("type", "text"));
        propertiesMap.put(properties.vectorField(), Map.of(
                "type", "dense_vector",
                "dims", properties.embeddingDimensions(),
                "index", true,
                "similarity", "cosine"
        ));
        mapping.put("mappings", Map.of("properties", propertiesMap));
        return objectMapper.writeValueAsString(mapping);
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
        body.putAll(chunk.metadata());
        return body;
    }
}
