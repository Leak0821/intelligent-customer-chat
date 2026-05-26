package com.leak.intelligentcustomerchat.app.workflow;

import java.time.OffsetDateTime;

public record WorkflowQueueItemView(
        String runId,
        String messageId,
        String threadId,
        String sender,
        String subject,
        String workflowStatus,
        String workflowStage,
        String draftStatus,
        String sendReadiness,
        String nextAction,
        String latestDispatchStatus,
        Integer latestDispatchAttemptCount,
        OffsetDateTime latestDispatchNextRetryAt,
        String latestReviewAction,
        String latestReviewer,
        String latestReviewNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
