package com.leak.intelligentcustomerchat.app.reply;

public record DispatchRetryBatchResult(
        int processedCount,
        int sentCount,
        int retryPendingCount,
        int failedFinalCount
) {
}
