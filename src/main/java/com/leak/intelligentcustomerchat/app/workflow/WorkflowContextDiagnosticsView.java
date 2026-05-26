package com.leak.intelligentcustomerchat.app.workflow;

public record WorkflowContextDiagnosticsView(
        boolean memoryEnabled,
        boolean compressionEnabled,
        boolean llmSummaryEnabled,
        int recentRoundLimit,
        int summaryThreshold,
        long totalMessageCount,
        int recentMessageCount,
        boolean compressionAttempted,
        boolean compressionSucceeded,
        String compressionDecision,
        String compressionSkipReason,
        String summaryResolutionSource,
        boolean restoredPersistedSummaryToMemory,
        boolean persistedSummaryCoversCurrentThread,
        WorkflowPersistedSummaryView latestPersistedSummary
) {
}
