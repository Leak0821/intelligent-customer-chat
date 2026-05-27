package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ElasticsearchHybridSearchServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldOnlySearchActiveChunkDocuments() {
        ElasticsearchHybridSearchService service = new ElasticsearchHybridSearchService(
                mock(RestClient.class),
                new ObjectMapper(),
                new KnowledgeElasticsearchProperties(
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
                ),
                new EmbeddingService() {
                    @Override
                    public float[] embed(String text) {
                        return new float[]{0.1f, 0.2f};
                    }

                    @Override
                    public int dimensions() {
                        return 2;
                    }
                },
                new RrfRankFusion()
        );

        Map<String, Object> bm25 = service.buildBm25Body(new RetrievalQuery("latest sales plan", "PRE_SALES", "general_inquiry", List.of(), 5));
        Map<String, Object> vector = service.buildVectorBody(new RetrievalQuery("latest sales plan", "PRE_SALES", "general_inquiry", List.of(), 5));

        List<Map<String, Object>> filters = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) bm25.get("query")).get("bool")).get("filter");
        Map<String, Object> knn = (Map<String, Object>) vector.get("knn");
        Map<String, Object> filterQuery = (Map<String, Object>) knn.get("filter");
        List<Map<String, Object>> vectorFilters = (List<Map<String, Object>>) ((Map<String, Object>) filterQuery.get("bool")).get("filter");

        assertThat(filters).contains(
                Map.of("term", Map.of("doc_type", "chunk")),
                Map.of("term", Map.of("status", "active"))
        );
        assertThat(vectorFilters).contains(
                Map.of("term", Map.of("doc_type", "chunk")),
                Map.of("term", Map.of("status", "active"))
        );
    }
}
