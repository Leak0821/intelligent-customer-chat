package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import org.springframework.stereotype.Service;

@Service
public class HeuristicIntentRoutingService implements IntentRoutingService {

    @Override
    public IntentRouteResult route(IntentNormalizationResult normalizationResult) {
        CustomerScene scene = normalizationResult.sceneCandidates().get(0);
        String subIntent = normalizationResult.subIntentCandidates().get(0);
        String reason = "route by first candidate scene=" + scene + ", subIntent=" + subIntent;
        return new IntentRouteResult(scene, subIntent, normalizationResult.disposition(), reason);
    }
}
