package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchKnowledgeIndexDefinitionFactoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildIndexDefinitionForHybridRetrieval() {
        KnowledgeElasticsearchProperties properties = new KnowledgeElasticsearchProperties(
                true,
                "http://127.0.0.1:9200",
                "knowledge_chunks",
                "title",
                "content",
                "content_vector",
                "parent_id",
                "doc_type",
                10,
                50,
                16,
                60
        );
        ElasticsearchKnowledgeIndexDefinitionFactory factory = new ElasticsearchKnowledgeIndexDefinitionFactory();

        Map<String, Object> definition = factory.build(properties);

        assertThat(definition).containsKeys("settings", "mappings");
        Map<String, Object> mappings = (Map<String, Object>) definition.get("mappings");
        Map<String, Object> propertyMap = (Map<String, Object>) mappings.get("properties");
        assertThat(propertyMap).containsKeys("doc_type", "parent_id", "chunk_order", "title", "content", "content_vector");
        assertThat((Map<String, Object>) propertyMap.get("content_vector"))
                .containsEntry("type", "dense_vector")
                .containsEntry("dims", 16)
                .containsEntry("index", true)
                .containsEntry("similarity", "cosine");
    }
}
