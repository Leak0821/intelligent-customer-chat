package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;

import java.util.List;
import java.util.Objects;

public record IntentNormalizationDiagnostics(
        IntentNormalizationResult heuristicBaseline,
        IntentNormalizationResult finalResult,
        String normalizationSource,
        boolean llmAttempted,
        boolean llmResponseAccepted,
        String fallbackReason,
        List<String> guardrailActions
) {
    public IntentNormalizationDiagnostics {
        Objects.requireNonNull(heuristicBaseline, "heuristicBaseline must not be null");
        Objects.requireNonNull(finalResult, "finalResult must not be null");
        Objects.requireNonNull(normalizationSource, "normalizationSource must not be null");
        guardrailActions = List.copyOf(guardrailActions);
    }
}
