package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;

public interface KnowledgeRetrieveService {
    KnowledgeRetrieveResult retrieve(IntentNormalizationResult normalizationResult,
                                     IntentRouteResult routeResult,
                                     BusinessFactResult businessFactResult);
}
