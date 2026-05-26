package com.leak.intelligentcustomerchat.app.workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record WorkflowEvaluationSampleView(
        String runId,
        String messageId,
        String threadId,
        String sender,
        String subject,
        String normalizationSummary,
        String routingSummary,
        boolean businessFactsTriggered,
        String businessFactsSummary,
        boolean knowledgeTriggered,
        String knowledgeSummary,
        String workflowStatus,
        String workflowStage,
        String workflowReason,
        String draftStatus,
        String sendReadiness,
        String nextAction,
        Integer draftVersion,
        String latestDispatchStatus,
        String latestReviewAction,
        String latestReviewer,
        String latestReviewNote,
        List<String> riskFlags,
        OffsetDateTime sampledAt
) {
    public WorkflowEvaluationSampleView {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(threadId, "threadId must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(normalizationSummary, "normalizationSummary must not be null");
        Objects.requireNonNull(routingSummary, "routingSummary must not be null");
        Objects.requireNonNull(businessFactsSummary, "businessFactsSummary must not be null");
        Objects.requireNonNull(knowledgeSummary, "knowledgeSummary must not be null");
        Objects.requireNonNull(workflowStatus, "workflowStatus must not be null");
        Objects.requireNonNull(workflowStage, "workflowStage must not be null");
        Objects.requireNonNull(workflowReason, "workflowReason must not be null");
        Objects.requireNonNull(riskFlags, "riskFlags must not be null");
        Objects.requireNonNull(sampledAt, "sampledAt must not be null");
        riskFlags = List.copyOf(riskFlags);
    }
}
