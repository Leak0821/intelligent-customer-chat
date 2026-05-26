package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;

public interface IntentRoutingService {
    IntentRouteResult route(IntentNormalizationResult normalizationResult);
}
