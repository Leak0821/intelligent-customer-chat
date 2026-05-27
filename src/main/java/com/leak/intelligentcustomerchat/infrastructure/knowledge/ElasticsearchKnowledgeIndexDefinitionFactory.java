package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ElasticsearchKnowledgeIndexDefinitionFactory {

    public Map<String, Object> build(KnowledgeElasticsearchProperties properties) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("settings", buildSettings());
        root.put("mappings", Map.of("dynamic", true, "properties", buildProperties(properties)));
        return root;
    }

    private Map<String, Object> buildSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("number_of_shards", 1);
        settings.put("number_of_replicas", 0);
        settings.put("refresh_interval", "1s");
        return settings;
    }

    private Map<String, Object> buildProperties(KnowledgeElasticsearchProperties properties) {
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put(properties.docTypeField(), Map.of("type", "keyword"));
        propertiesMap.put(properties.parentIdField(), Map.of("type", "keyword"));
        propertiesMap.put("chunk_order", Map.of("type", "integer"));
        propertiesMap.put("knowledge_key", Map.of("type", "keyword"));
        propertiesMap.put("version", Map.of("type", "keyword"));
        propertiesMap.put("status", Map.of("type", "keyword"));
        propertiesMap.put("content_hash", Map.of("type", "keyword"));
        propertiesMap.put("chunk_hash", Map.of("type", "keyword"));
        propertiesMap.put("superseded_by_version", Map.of("type", "keyword"));
        propertiesMap.put("superseded_at", Map.of("type", "date"));
        propertiesMap.put(properties.titleField(), textWithKeywordField());
        propertiesMap.put(properties.contentField(), Map.of("type", "text"));
        propertiesMap.put(properties.vectorField(), Map.of(
                "type", "dense_vector",
                "dims", properties.embeddingDimensions(),
                "index", true,
                "similarity", "cosine"
        ));
        return propertiesMap;
    }

    private Map<String, Object> textWithKeywordField() {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "text");
        field.put("fields", Map.of(
                "keyword", Map.of(
                        "type", "keyword",
                        "ignore_above", 256
                )
        ));
        return field;
    }
}
