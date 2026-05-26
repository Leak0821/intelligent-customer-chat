package com.leak.intelligentcustomerchat.app.workflow;

public record WorkflowContextDiagnosticsView(
        boolean memoryEnabled,
        boolean compressionEnabled,
        boolean llmSummaryEnabled,
        int recentRoundLimit,
        int summaryThreshold,
        long totalMessageCount,
        int recentMessageCount,
        boolean persistedSummaryCoversCurrentThread,
        WorkflowPersistedSummaryView latestPersistedSummary
) {
}
