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
        String scene,
        String subIntent,
        String routingSummary,
        boolean businessFactsTriggered,
        String businessFactsSummary,
        String businessFactStatus,
        String businessFactRole,
        List<String> businessFactSourceSystems,
        boolean knowledgeTriggered,
        String knowledgeSummary,
        String knowledgeRole,
        String knowledgeRetrievalSource,
        Integer knowledgeRecallCount,
        List<String> knowledgeSnippetIds,
        String workflowStatus,
        String workflowStage,
        String workflowReason,
        String draftStatus,
        String replySource,
        String replyFallbackReason,
        String sendReadiness,
        String nextAction,
        Integer draftVersion,
        String latestDispatchStatus,
        String latestReviewAction,
        String latestReviewer,
        String latestReviewNote,
        Integer reviewCount,
        Integer revisionCount,
        boolean resubmittedForReview,
        List<String> reviewTimeline,
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
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(subIntent, "subIntent must not be null");
        Objects.requireNonNull(routingSummary, "routingSummary must not be null");
        Objects.requireNonNull(businessFactsSummary, "businessFactsSummary must not be null");
        Objects.requireNonNull(businessFactStatus, "businessFactStatus must not be null");
        Objects.requireNonNull(businessFactRole, "businessFactRole must not be null");
        Objects.requireNonNull(businessFactSourceSystems, "businessFactSourceSystems must not be null");
        Objects.requireNonNull(knowledgeSummary, "knowledgeSummary must not be null");
        Objects.requireNonNull(knowledgeRole, "knowledgeRole must not be null");
        Objects.requireNonNull(knowledgeRetrievalSource, "knowledgeRetrievalSource must not be null");
        Objects.requireNonNull(knowledgeSnippetIds, "knowledgeSnippetIds must not be null");
        Objects.requireNonNull(workflowStatus, "workflowStatus must not be null");
        Objects.requireNonNull(workflowStage, "workflowStage must not be null");
        Objects.requireNonNull(workflowReason, "workflowReason must not be null");
        Objects.requireNonNull(replySource, "replySource must not be null");
        Objects.requireNonNull(reviewTimeline, "reviewTimeline must not be null");
        Objects.requireNonNull(riskFlags, "riskFlags must not be null");
        Objects.requireNonNull(sampledAt, "sampledAt must not be null");
        businessFactSourceSystems = List.copyOf(businessFactSourceSystems);
        knowledgeSnippetIds = List.copyOf(knowledgeSnippetIds);
        reviewTimeline = List.copyOf(reviewTimeline);
        riskFlags = List.copyOf(riskFlags);
    }
}
