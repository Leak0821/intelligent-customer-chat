package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.HybridRetrievalResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchKnowledgeRetriever implements KnowledgeRetriever {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchKnowledgeRetriever.class);
    private final HybridSearchService hybridSearchService;

    public ElasticsearchKnowledgeRetriever(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @Override
    public KnowledgeRetrieveResult retrieve(RetrievalQuery query) {
        try {
            HybridRetrievalResult result = hybridSearchService.search(query);
            return new KnowledgeRetrieveResult("elasticsearch-hybrid", result.snippets(), result.snippets().size());
        } catch (RuntimeException ex) {
            log.warn("hybrid retrieval from elasticsearch failed: {}", ex.getMessage());
            return KnowledgeRetrieveResult.empty();
        }
    }
}
