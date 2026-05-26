package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;

import java.util.Objects;

public record ContextLoadingDiagnostics(
        ContextSnapshot snapshot,
        long totalMessageCount,
        int recentMessageCount,
        boolean compressionAttempted,
        boolean compressionSucceeded,
        String compressionDecision,
        String compressionSkipReason,
        String summaryResolutionSource,
        boolean restoredPersistedSummaryToMemory
) {
    public ContextLoadingDiagnostics {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(compressionDecision, "compressionDecision must not be null");
        Objects.requireNonNull(summaryResolutionSource, "summaryResolutionSource must not be null");
    }
}
