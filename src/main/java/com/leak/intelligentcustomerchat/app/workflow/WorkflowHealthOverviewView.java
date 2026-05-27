package com.leak.intelligentcustomerchat.app.workflow;

import java.util.Objects;

public record WorkflowHealthOverviewView(
        int sampledCount,
        int sendAllowedCount,
        int holdForReviewCount,
        int followUpCount,
        int retryPendingCount,
        int manualInterventionCount,
        int blockedCount,
        int lowRiskCount,
        int mediumRiskCount,
        int highRiskCount,
        int criticalRiskCount,
        String overviewStatus,
        String overviewMessage
) {
    public WorkflowHealthOverviewView {
        Objects.requireNonNull(overviewStatus, "overviewStatus must not be null");
        Objects.requireNonNull(overviewMessage, "overviewMessage must not be null");
    }
}
