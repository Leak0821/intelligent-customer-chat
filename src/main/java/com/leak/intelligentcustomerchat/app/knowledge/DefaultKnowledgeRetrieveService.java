package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeRetriever;
import org.springframework.stereotype.Service;

@Service
public class DefaultKnowledgeRetrieveService implements KnowledgeRetrieveService {
    private final KnowledgeRetriever knowledgeRetriever;
    private final RetrievalConfigService retrievalConfigService;
    private final KnowledgeRetrievalQueryBuilder knowledgeRetrievalQueryBuilder;

    public DefaultKnowledgeRetrieveService(KnowledgeRetriever knowledgeRetriever,
                                           RetrievalConfigService retrievalConfigService,
                                           KnowledgeRetrievalQueryBuilder knowledgeRetrievalQueryBuilder) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.retrievalConfigService = retrievalConfigService;
        this.knowledgeRetrievalQueryBuilder = knowledgeRetrievalQueryBuilder;
    }

    @Override
    public KnowledgeRetrieveResult retrieve(IntentNormalizationResult normalizationResult,
                                            IntentRouteResult routeResult,
                                            ContextSnapshot contextSnapshot,
                                            BusinessFactResult businessFactResult) {
        RetrievalQuery query = knowledgeRetrievalQueryBuilder.build(
                normalizationResult,
                routeResult,
                contextSnapshot,
                businessFactResult,
                retrievalConfigService.currentRetrievalSettings()
        );
        return knowledgeRetriever.retrieve(query);
    }
}
