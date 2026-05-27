package com.leak.intelligentcustomerchat.app.workflow;

import java.util.List;
import java.util.Objects;

public record DemoScenarioExecutionSummaryView(
        String runId,
        String mode,
        String demoTakeaway,
        String scene,
        String subIntent,
        String resultType,
        String workflowStatus,
        String draftStatus,
        String operatorDecision,
        String nextAction,
        String businessFactStatus,
        String businessEvidence,
        String knowledgeEvidence,
        String replyEvidence,
        List<String> keyEvidence
) {
    public DemoScenarioExecutionSummaryView {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(demoTakeaway, "demoTakeaway must not be null");
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(subIntent, "subIntent must not be null");
        Objects.requireNonNull(resultType, "resultType must not be null");
        Objects.requireNonNull(workflowStatus, "workflowStatus must not be null");
        Objects.requireNonNull(draftStatus, "draftStatus must not be null");
        Objects.requireNonNull(operatorDecision, "operatorDecision must not be null");
        Objects.requireNonNull(nextAction, "nextAction must not be null");
        Objects.requireNonNull(businessFactStatus, "businessFactStatus must not be null");
        Objects.requireNonNull(businessEvidence, "businessEvidence must not be null");
        Objects.requireNonNull(knowledgeEvidence, "knowledgeEvidence must not be null");
        Objects.requireNonNull(replyEvidence, "replyEvidence must not be null");
        Objects.requireNonNull(keyEvidence, "keyEvidence must not be null");
        keyEvidence = List.copyOf(keyEvidence);
    }
}
