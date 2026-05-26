package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;

public interface BusinessFactService {
    BusinessFactResult loadFacts(IntentNormalizationResult normalizationResult,
                                 IntentRouteResult routeResult,
                                 ContextSnapshot contextSnapshot);
}
