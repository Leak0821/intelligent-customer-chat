package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.app.context.ContextEntitySignalExtractor;
import com.leak.intelligentcustomerchat.app.context.ContextEntitySignals;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContextAwareIntentNormalizationService {
    private final ContextEntitySignalExtractor contextEntitySignalExtractor;

    public ContextAwareIntentNormalizationService(ContextEntitySignalExtractor contextEntitySignalExtractor) {
        this.contextEntitySignalExtractor = contextEntitySignalExtractor;
    }

    public ContextAwareIntentNormalizationResult enrich(IntentNormalizationResult normalizationResult, ContextSnapshot contextSnapshot) {
        ContextEntitySignals contextEntitySignals = contextEntitySignalExtractor.extract(contextSnapshot);
        if (!normalizationResult.missingEntities().contains(IntentNormalizationHeuristics.ORDER_ID_ENTITY)
                || !contextEntitySignals.hasReusableIdentifier()) {
            return new ContextAwareIntentNormalizationResult(normalizationResult, List.of());
        }

        List<String> updatedMissingEntities = normalizationResult.missingEntities().stream()
                .filter(value -> !IntentNormalizationHeuristics.ORDER_ID_ENTITY.equals(value))
                .toList();
        ProcessingDisposition updatedDisposition = normalizationResult.disposition() == ProcessingDisposition.FOLLOW_UP
                ? ProcessingDisposition.CONTINUE
                : normalizationResult.disposition();

        List<String> actions = new ArrayList<>();
        if (contextEntitySignals.orderId() != null && !contextEntitySignals.orderId().isBlank()) {
            actions.add("reuse_context_order_id");
        }
        if (contextEntitySignals.trackingNumber() != null && !contextEntitySignals.trackingNumber().isBlank()) {
            actions.add("reuse_context_tracking_number");
        }

        return new ContextAwareIntentNormalizationResult(
                new IntentNormalizationResult(
                        normalizationResult.normalizedRequest(),
                        normalizationResult.primaryQuestion(),
                        normalizationResult.secondaryQuestions(),
                        normalizationResult.sceneCandidates(),
                        normalizationResult.subIntentCandidates(),
                        normalizationResult.requiredEntities(),
                        updatedMissingEntities,
                        updatedDisposition
                ),
                List.copyOf(actions)
        );
    }
}
