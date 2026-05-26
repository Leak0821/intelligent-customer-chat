package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchKnowledgeRetriever implements KnowledgeRetriever {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchKnowledgeRetriever.class);

    private final ElasticsearchClient elasticsearchClient;
    private final KnowledgeElasticsearchProperties properties;

    public ElasticsearchKnowledgeRetriever(ElasticsearchClient elasticsearchClient,
                                           KnowledgeElasticsearchProperties properties) {
        this.elasticsearchClient = elasticsearchClient;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public KnowledgeRetrieveResult retrieve(RetrievalQuery query) {
        try {
            SearchResponse<Map> response = elasticsearchClient.search(search -> search
                            .index(properties.indexName())
                            .size(Math.min(query.topK(), properties.topK()))
                            .query(buildQuery(query)),
                    Map.class);

            List<KnowledgeSnippet> snippets = response.hits().hits().stream()
                    .map(hit -> toSnippet(hit.id(), hit.score(), hit.source()))
                    .filter(snippet -> snippet != null)
                    .toList();
            return new KnowledgeRetrieveResult("elasticsearch", snippets, snippets.size());
        } catch (IOException ex) {
            log.warn("knowledge retrieval from elasticsearch failed: {}", ex.getMessage());
            return KnowledgeRetrieveResult.empty();
        }
    }

    private Query buildQuery(RetrievalQuery query) {
        return Query.of(root -> root.bool(bool -> bool
                .should(should -> should.multiMatch(multi -> multi
                        .query(query.queryText())
                        .fields(properties.titleField(), properties.contentField())
                ))
                .should(should -> should.match(match -> match
                        .field(properties.contentField())
                        .query(query.subIntent())
                ))
        ));
    }

    private KnowledgeSnippet toSnippet(String id, Double score, Map source) {
        if (source == null) {
            return null;
        }
        Object title = source.getOrDefault(properties.titleField(), "knowledge-item");
        Object content = source.getOrDefault(properties.contentField(), "");
        return new KnowledgeSnippet(
                id == null ? UUID.randomUUID().toString() : id,
                String.valueOf(title),
                String.valueOf(content),
                score == null ? 0.0d : score,
                "elasticsearch"
        );
    }
}
