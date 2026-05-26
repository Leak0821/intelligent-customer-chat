package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;

import java.util.Objects;

public record WorkflowIntentDiagnosticsView(
        IntentNormalizationResult heuristicBaseline,
        IntentCatalogConfig intentCatalog,
        boolean normalizationChangedByModel,
        String normalizationSource,
        boolean llmAttempted,
        boolean llmResponseAccepted,
        String fallbackReason,
        java.util.List<String> guardrailActions
) {
    public WorkflowIntentDiagnosticsView {
        Objects.requireNonNull(heuristicBaseline, "heuristicBaseline must not be null");
        Objects.requireNonNull(intentCatalog, "intentCatalog must not be null");
        Objects.requireNonNull(normalizationSource, "normalizationSource must not be null");
        guardrailActions = java.util.List.copyOf(guardrailActions);
    }
}
