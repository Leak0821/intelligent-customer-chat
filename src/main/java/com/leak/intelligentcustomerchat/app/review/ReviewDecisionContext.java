package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;

import java.util.Objects;

public record ReviewDecisionContext(
        IntentRouteResult routeResult,
        BusinessFactResult businessFactResult,
        KnowledgeRetrieveResult knowledgeRetrieveResult
) {
    public ReviewDecisionContext {
        Objects.requireNonNull(routeResult, "routeResult must not be null");
        Objects.requireNonNull(businessFactResult, "businessFactResult must not be null");
        Objects.requireNonNull(knowledgeRetrieveResult, "knowledgeRetrieveResult must not be null");
    }
}
