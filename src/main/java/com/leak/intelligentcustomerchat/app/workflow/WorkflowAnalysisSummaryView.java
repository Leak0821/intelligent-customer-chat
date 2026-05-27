package com.leak.intelligentcustomerchat.app.workflow;

import java.util.List;
import java.util.Objects;

public record WorkflowAnalysisSummaryView(
        String scene,
        String subIntent,
        String disposition,
        String finalStatus,
        String operatorDecision,
        String nextAction,
        String primaryQuestion,
        String intentSummary,
        String contextSummary,
        String factSummary,
        String knowledgeSummary,
        String replySummary,
        List<String> keyEvidence
) {
    public WorkflowAnalysisSummaryView {
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(subIntent, "subIntent must not be null");
        Objects.requireNonNull(disposition, "disposition must not be null");
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");
        Objects.requireNonNull(operatorDecision, "operatorDecision must not be null");
        Objects.requireNonNull(nextAction, "nextAction must not be null");
        Objects.requireNonNull(primaryQuestion, "primaryQuestion must not be null");
        Objects.requireNonNull(intentSummary, "intentSummary must not be null");
        Objects.requireNonNull(contextSummary, "contextSummary must not be null");
        Objects.requireNonNull(factSummary, "factSummary must not be null");
        Objects.requireNonNull(knowledgeSummary, "knowledgeSummary must not be null");
        Objects.requireNonNull(replySummary, "replySummary must not be null");
        Objects.requireNonNull(keyEvidence, "keyEvidence must not be null");
        keyEvidence = List.copyOf(keyEvidence);
    }
}
