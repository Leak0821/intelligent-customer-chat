package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

public interface BusinessFactService {
    BusinessFactResult loadFacts(InboundMail mail,
                                 IntentNormalizationResult normalizationResult,
                                 IntentRouteResult routeResult,
                                 ContextSnapshot contextSnapshot);
}
