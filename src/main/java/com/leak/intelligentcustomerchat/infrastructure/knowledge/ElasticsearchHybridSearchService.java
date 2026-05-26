package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import com.leak.intelligentcustomerchat.domain.knowledge.HybridRetrievalResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchHybridSearchService implements HybridSearchService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KnowledgeElasticsearchProperties properties;
    private final EmbeddingService embeddingService;
    private final RrfRankFusion rrfRankFusion;

    public ElasticsearchHybridSearchService(RestClient restClient,
                                            ObjectMapper objectMapper,
                                            KnowledgeElasticsearchProperties properties,
                                            EmbeddingService embeddingService,
                                            RrfRankFusion rrfRankFusion) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.rrfRankFusion = rrfRankFusion;
    }

    @Override
    public HybridRetrievalResult search(RetrievalQuery query) {
        List<KnowledgeSnippet> bm25 = searchByBm25(query);
        List<KnowledgeSnippet> vector = searchByVector(query);
        List<KnowledgeSnippet> fused = rrfRankFusion.fuse(bm25, vector, properties.rrfK(), Math.min(query.topK(), properties.topK()));
        return new HybridRetrievalResult(fused, bm25, vector);
    }

    private List<KnowledgeSnippet> searchByBm25(RetrievalQuery query) {
        Map<String, Object> body = Map.of(
                "size", Math.min(query.topK(), properties.topK()),
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(Map.of("term", Map.of(properties.docTypeField(), "chunk"))),
                                "should", List.of(
                                        Map.of("multi_match", Map.of(
                                                "query", query.queryText(),
                                                "fields", List.of(properties.titleField() + "^2", properties.contentField())
                                        )),
                                        Map.of("match", Map.of(properties.contentField(), Map.of("query", query.subIntent())))
                                ),
                                "minimum_should_match", 1
                        )
                )
        );
        return executeSearch(body);
    }

    private List<KnowledgeSnippet> searchByVector(RetrievalQuery query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", Math.min(query.topK(), properties.topK()));
        body.put("knn", Map.of(
                "field", properties.vectorField(),
                "query_vector", embeddingService.embed(query.queryText()),
                "k", Math.min(query.topK(), properties.topK()),
                "num_candidates", properties.numCandidates(),
                "filter", Map.of("term", Map.of(properties.docTypeField(), "chunk"))
        ));
        return executeSearch(body);
    }

    private List<KnowledgeSnippet> executeSearch(Map<String, Object> body) {
        try {
            Request request = new Request("POST", "/" + properties.indexName() + "/_search");
            request.setJsonEntity(objectMapper.writeValueAsString(body));
            JsonNode root = objectMapper.readTree(restClient.performRequest(request).getEntity().getContent());
            List<KnowledgeSnippet> snippets = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                JsonNode source = hit.path("_source");
                snippets.add(new KnowledgeSnippet(
                        hit.path("_id").asText(UUID.randomUUID().toString()),
                        source.path(properties.titleField()).asText("knowledge-item"),
                        source.path(properties.contentField()).asText(""),
                        hit.path("_score").asDouble(0.0d),
                        source.path(properties.parentIdField()).asText("elasticsearch")
                ));
            }
            return snippets;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to search knowledge from elasticsearch", ex);
        }
    }
}
