package com.leak.intelligentcustomerchat.app.workflow;

import java.util.List;
import java.util.Objects;

public record WorkflowRiskDecisionView(
        String riskLevel,
        String releaseDecision,
        boolean sendAllowed,
        String recommendedAction,
        List<String> blockingReasons,
        List<String> decisionSignals
) {
    public WorkflowRiskDecisionView {
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(releaseDecision, "releaseDecision must not be null");
        Objects.requireNonNull(recommendedAction, "recommendedAction must not be null");
        Objects.requireNonNull(blockingReasons, "blockingReasons must not be null");
        Objects.requireNonNull(decisionSignals, "decisionSignals must not be null");
        blockingReasons = List.copyOf(blockingReasons);
        decisionSignals = List.copyOf(decisionSignals);
    }
}
