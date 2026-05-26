package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.app.config.IntentConfigService;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import org.springframework.stereotype.Service;

@Service
public class HeuristicIntentRoutingService implements IntentRoutingService {
    private final IntentConfigService intentConfigService;

    public HeuristicIntentRoutingService(IntentConfigService intentConfigService) {
        this.intentConfigService = intentConfigService;
    }

    @Override
    public IntentRouteResult route(IntentNormalizationResult normalizationResult) {
        CustomerScene scene = normalizationResult.sceneCandidates().get(0);
        String candidateIntent = normalizationResult.subIntentCandidates().get(0);
        String subIntent = normalizeIntent(scene, candidateIntent);
        String reason = "route by first candidate scene=" + scene + ", subIntent=" + subIntent;
        return new IntentRouteResult(scene, subIntent, normalizationResult.disposition(), reason);
    }

    private String normalizeIntent(CustomerScene scene, String candidateIntent) {
        return switch (scene) {
            case PRE_SALES -> intentConfigService.currentIntentCatalog().preSalesIntents().contains(candidateIntent)
                    ? candidateIntent
                    : "general_inquiry";
            case AFTER_SALES -> intentConfigService.currentIntentCatalog().afterSalesIntents().contains(candidateIntent)
                    ? candidateIntent
                    : "general_inquiry";
            default -> "general_inquiry";
        };
    }
}
