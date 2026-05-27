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
        List<WorkflowEvaluationCountView> riskFlags,
        OffsetDateTime sampledAt
) {
    public WorkflowEvaluationSummaryView {
        Objects.requireNonNull(scenes, "scenes must not be null");
        Objects.requireNonNull(subIntents, "subIntents must not be null");
        Objects.requireNonNull(workflowStatuses, "workflowStatuses must not be null");
        Objects.requireNonNull(draftStatuses, "draftStatuses must not be null");
        Objects.requireNonNull(replySources, "replySources must not be null");
        Objects.requireNonNull(riskFlags, "riskFlags must not be null");
        Objects.requireNonNull(sampledAt, "sampledAt must not be null");
        scenes = List.copyOf(scenes);
        subIntents = List.copyOf(subIntents);
        workflowStatuses = List.copyOf(workflowStatuses);
        draftStatuses = List.copyOf(draftStatuses);
        replySources = List.copyOf(replySources);
        riskFlags = List.copyOf(riskFlags);
    }
}
