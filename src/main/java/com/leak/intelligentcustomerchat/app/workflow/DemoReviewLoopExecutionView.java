package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;

import java.util.List;
import java.util.Objects;

public record DemoReviewLoopExecutionView(
        WorkflowRun run,
        DemoReviewLoopDraftView initialDraft,
        DemoReviewLoopDraftView rejectedDraft,
        DemoReviewLoopDraftView resubmittedDraft,
        DemoReviewLoopDraftView approvedDraft,
        WorkflowReplayView replay,
        WorkflowEvaluationSampleView evaluation,
        List<ReviewRecord> reviews
) {
    public DemoReviewLoopExecutionView {
        Objects.requireNonNull(run, "run must not be null");
        Objects.requireNonNull(initialDraft, "initialDraft must not be null");
        Objects.requireNonNull(rejectedDraft, "rejectedDraft must not be null");
        Objects.requireNonNull(resubmittedDraft, "resubmittedDraft must not be null");
        Objects.requireNonNull(approvedDraft, "approvedDraft must not be null");
        Objects.requireNonNull(replay, "replay must not be null");
        Objects.requireNonNull(evaluation, "evaluation must not be null");
        reviews = List.copyOf(reviews);
    }
}
