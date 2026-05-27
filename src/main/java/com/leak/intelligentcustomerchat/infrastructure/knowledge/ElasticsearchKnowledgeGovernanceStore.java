package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchKnowledgeGovernanceStore implements KnowledgeGovernanceStore {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KnowledgeElasticsearchProperties properties;

    public ElasticsearchKnowledgeGovernanceStore(RestClient restClient,
                                                 ObjectMapper objectMapper,
                                                 KnowledgeElasticsearchProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void validateImport(String knowledgeKey, String version, String contentHash) {
        if (parentExists(List.of(
                termFilter(properties.docTypeField(), "parent"),
                termFilter("knowledge_key", knowledgeKey),
                termFilter("version", version)
        ))) {
            throw new IllegalArgumentException("已存在相同 knowledge_key 和 version 的知识文档: " + knowledgeKey + " / " + version);
        }
        if (parentExists(List.of(
                termFilter(properties.docTypeField(), "parent"),
                termFilter("knowledge_key", knowledgeKey),
                termFilter("content_hash", contentHash)
        ))) {
            throw new IllegalArgumentException("已存在相同 knowledge_key 和 content_hash 的重复知识内容: " + knowledgeKey);
        }
    }

    @Override
    public int deprecateOlderActiveVersions(String knowledgeKey, String currentVersion) {
        List<String> documentIds = findDocumentIds(List.of(
                termFilter("knowledge_key", knowledgeKey),
                termFilter("status", "active")
        ), List.of(
                termFilter("version", currentVersion)
        ));
        if (documentIds.isEmpty()) {
            return 0;
        }
        return bulkUpdateStatus(documentIds, "deprecated", currentVersion);
    }

    private boolean parentExists(List<Map<String, Object>> filters) {
        return !findDocumentIds(filters, List.of(), 1).isEmpty();
    }

    private List<String> findDocumentIds(List<Map<String, Object>> filters, List<Map<String, Object>> mustNotFilters) {
        return findDocumentIds(filters, mustNotFilters, 1000);
    }

    private List<String> findDocumentIds(List<Map<String, Object>> filters,
                                         List<Map<String, Object>> mustNotFilters,
                                         int size) {
        Map<String, Object> boolQuery = new LinkedHashMap<>();
        boolQuery.put("filter", filters);
        if (!mustNotFilters.isEmpty()) {
            boolQuery.put("must_not", mustNotFilters);
        }
        Map<String, Object> body = Map.of(
                "size", size,
                "_source", false,
                "query", Map.of("bool", boolQuery)
        );
        try {
            Request request = new Request("POST", "/" + properties.indexName() + "/_search");
            request.setJsonEntity(objectMapper.writeValueAsString(body));
            JsonNode root = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            List<String> ids = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                ids.add(hit.path("_id").asText());
            }
            return ids;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to inspect knowledge governance records from elasticsearch", ex);
        }
    }

    private int bulkUpdateStatus(List<String> ids, String status, String supersededByVersion) {
        String now = OffsetDateTime.now().toString();
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            builder.append("{\"update\":{\"_index\":\"")
                    .append(properties.indexName())
                    .append("\",\"_id\":\"")
                    .append(id)
                    .append("\"}}\n");
            builder.append("{\"doc\":{\"status\":\"")
                    .append(status)
                    .append("\",\"superseded_by_version\":\"")
                    .append(supersededByVersion)
                    .append("\",\"superseded_at\":\"")
                    .append(now)
                    .append("\"}}\n");
        }
        try {
            Request request = new Request("POST", "/_bulk");
            request.addParameter("refresh", "true");
            request.setEntity(new NStringEntity(builder.toString(), ContentType.create("application/x-ndjson", StandardCharsets.UTF_8)));
            JsonNode root = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            if (root.path("errors").asBoolean(false)) {
                throw new IllegalStateException("failed to bulk update deprecated knowledge documents");
            }
            return ids.size();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to bulk update knowledge governance records", ex);
        }
    }

    private Map<String, Object> termFilter(String field, String value) {
        return Map.of("term", Map.of(field, value));
    }
}
