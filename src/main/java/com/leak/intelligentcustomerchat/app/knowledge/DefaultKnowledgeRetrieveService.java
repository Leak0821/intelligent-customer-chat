package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
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

    public DefaultKnowledgeRetrieveService(KnowledgeRetriever knowledgeRetriever,
                                           RetrievalConfigService retrievalConfigService) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.retrievalConfigService = retrievalConfigService;
    }

    @Override
    public KnowledgeRetrieveResult retrieve(IntentNormalizationResult normalizationResult,
                                            IntentRouteResult routeResult,
                                            BusinessFactResult businessFactResult) {
        RetrievalQuery query = new RetrievalQuery(
                normalizationResult.normalizedRequest(),
                routeResult.scene().name(),
                routeResult.subIntent(),
                businessFactResult.resolvedEntities(),
                retrievalConfigService.currentRetrievalSettings().topK()
        );
        return knowledgeRetriever.retrieve(query);
    }
}
