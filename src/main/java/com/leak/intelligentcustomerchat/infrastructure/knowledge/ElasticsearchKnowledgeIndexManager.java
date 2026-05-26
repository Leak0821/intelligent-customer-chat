package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchKnowledgeIndexManager {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KnowledgeElasticsearchProperties properties;
    private final ElasticsearchKnowledgeIndexDefinitionFactory definitionFactory;

    public ElasticsearchKnowledgeIndexManager(RestClient restClient,
                                              ObjectMapper objectMapper,
                                              KnowledgeElasticsearchProperties properties,
                                              ElasticsearchKnowledgeIndexDefinitionFactory definitionFactory) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.definitionFactory = definitionFactory;
    }

    public KnowledgeIndexStatus status() {
        return new KnowledgeIndexStatus(properties.indexName(), true, indexExists());
    }

    public KnowledgeIndexStatus ensureIndex() {
        if (indexExists()) {
            return new KnowledgeIndexStatus(properties.indexName(), true, true);
        }
        try {
            Request request = new Request("PUT", "/" + properties.indexName());
            request.setJsonEntity(objectMapper.writeValueAsString(definitionFactory.build(properties)));
            restClient.performRequest(request);
            return new KnowledgeIndexStatus(properties.indexName(), true, true);
        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == 400 && indexExists()) {
                return new KnowledgeIndexStatus(properties.indexName(), true, true);
            }
            throw new IllegalStateException("failed to create elasticsearch knowledge index", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create elasticsearch knowledge index", ex);
        }
    }

    private boolean indexExists() {
        try {
            restClient.performRequest(new Request("HEAD", "/" + properties.indexName()));
            return true;
        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw new IllegalStateException("failed to inspect elasticsearch knowledge index", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to inspect elasticsearch knowledge index", ex);
        }
    }

    public record KnowledgeIndexStatus(
            String indexName,
            boolean elasticsearchEnabled,
            boolean exists
    ) {
    }
}
