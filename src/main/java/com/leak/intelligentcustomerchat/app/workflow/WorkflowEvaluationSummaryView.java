package com.leak.intelligentcustomerchat.app.workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record WorkflowEvaluationSummaryView(
        int requestedLimit,
        int sampledCount,
        List<WorkflowEvaluationCountView> scenes,
        List<WorkflowEvaluationCountView> subIntents,
        List<WorkflowEvaluationCountView> workflowStatuses,
        List<WorkflowEvaluationCountView> draftStatuses,
        List<WorkflowEvaluationCountView> replySources,
        List<WorkflowEvaluationCountView> businessFactStatuses,
        List<WorkflowEvaluationCountView> businessFactRoles,
        List<WorkflowEvaluationCountView> knowledgeRoles,
        List<WorkflowEvaluationCountView> knowledgeRetrievalSources,
        List<WorkflowEvaluationCountView> replyFallbackReasons,
        List<WorkflowEvaluationCountView> latestReviewActions,
        List<WorkflowEvaluationCountView> manualReviewOutcomes,
        List<WorkflowEvaluationCountView> riskFlags,
        OffsetDateTime sampledAt
) {
    public WorkflowEvaluationSummaryView {
        Objects.requireNonNull(scenes, "scenes must not be null");
        Objects.requireNonNull(subIntents, "subIntents must not be null");
        Objects.requireNonNull(workflowStatuses, "workflowStatuses must not be null");
        Objects.requireNonNull(draftStatuses, "draftStatuses must not be null");
        Objects.requireNonNull(replySources, "replySources must not be null");
        Objects.requireNonNull(businessFactStatuses, "businessFactStatuses must not be null");
        Objects.requireNonNull(businessFactRoles, "businessFactRoles must not be null");
        Objects.requireNonNull(knowledgeRoles, "knowledgeRoles must not be null");
        Objects.requireNonNull(knowledgeRetrievalSources, "knowledgeRetrievalSources must not be null");
        Objects.requireNonNull(replyFallbackReasons, "replyFallbackReasons must not be null");
        Objects.requireNonNull(latestReviewActions, "latestReviewActions must not be null");
        Objects.requireNonNull(manualReviewOutcomes, "manualReviewOutcomes must not be null");
        Objects.requireNonNull(riskFlags, "riskFlags must not be null");
        Objects.requireNonNull(sampledAt, "sampledAt must not be null");
        scenes = List.copyOf(scenes);
        subIntents = List.copyOf(subIntents);
        workflowStatuses = List.copyOf(workflowStatuses);
        draftStatuses = List.copyOf(draftStatuses);
        replySources = List.copyOf(replySources);
        businessFactStatuses = List.copyOf(businessFactStatuses);
        businessFactRoles = List.copyOf(businessFactRoles);
        knowledgeRoles = List.copyOf(knowledgeRoles);
        knowledgeRetrievalSources = List.copyOf(knowledgeRetrievalSources);
        replyFallbackReasons = List.copyOf(replyFallbackReasons);
        latestReviewActions = List.copyOf(latestReviewActions);
        manualReviewOutcomes = List.copyOf(manualReviewOutcomes);
        riskFlags = List.copyOf(riskFlags);
    }
}
