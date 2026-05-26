package com.leak.intelligentcustomerchat.app.knowledge;

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

    public DefaultKnowledgeRetrieveService(KnowledgeRetriever knowledgeRetriever) {
        this.knowledgeRetriever = knowledgeRetriever;
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
                10
        );
        return knowledgeRetriever.retrieve(query);
    }
}
