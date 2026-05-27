package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowCaseViewTest {

    @Test
    void shouldExposeReviewActionsForPendingReviewDraft() {
        WorkflowEvaluationSampleView evaluation = sample(
                "DRAFT_READY",
                "PENDING_REVIEW",
                "await_review_decision",
                null,
                "NOT_REVIEWED",
                riskDecision("MEDIUM", "HOLD_FOR_REVIEW", false, "manual_review_required")
        );
        ReplyDraft draft = ReplyDraft.create("run-1", "Subject", "Draft body", ReplyDraftStatus.DRAFT_READY, "awaiting review");
        draft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "await_review_decision", "awaiting review");

        WorkflowCaseView view = WorkflowCaseView.from(evaluation, draft);

        assertThat(view.availableActions()).containsExactly("APPROVE_SEND", "REJECT_SEND", "REVISE_DRAFT");
        assertThat(view.summaryMessage()).isEqualTo("系统已生成回复草稿，当前等待人工审核。");
        assertThat(view.draftBody()).isEqualTo("Draft body");
    }

    @Test
    void shouldExposeDispatchActionForApprovedDraft() {
        WorkflowEvaluationSampleView evaluation = sample(
                "DRAFT_READY",
                "READY_FOR_SEND",
                "dispatch_reply",
                null,
                "APPROVED_FOR_SEND",
                riskDecision("LOW", "ALLOW_SEND", true, "dispatch_reply")
        );
        ReplyDraft draft = ReplyDraft.create("run-1", "Approved", "Ready to send", ReplyDraftStatus.DRAFT_READY, "approved");
        draft.updateSendReadiness(SendReadiness.READY_FOR_SEND, "dispatch_reply", "approved");

        WorkflowCaseView view = WorkflowCaseView.from(evaluation, draft);

        assertThat(view.availableActions()).containsExactly("DISPATCH");
        assertThat(view.summaryMessage()).isEqualTo("系统已完成审核放行，当前可以执行发送。");
    }

    private WorkflowEvaluationSampleView sample(String draftStatus,
                                                String sendReadiness,
                                                String nextAction,
                                                String latestDispatchStatus,
                                                String manualReviewOutcome,
                                                WorkflowRiskDecisionView riskDecision) {
        return new WorkflowEvaluationSampleView(
                "run-1",
                "msg-1",
                "thread-1",
                "customer@example.com",
                "Where is my order?",
                "primaryQuestion=where is my order",
                "AFTER_SALES",
                "ORDER_STATUS_QUERY",
                "scene=AFTER_SALES, subIntent=ORDER_STATUS_QUERY",
                true,
                "factStatus=FOUND",
                "FOUND",
                "business facts answer current order status",
                List.of("local-order-catalog"),
                true,
                "knowledgeRecallCount=1",
                "knowledge supplements explanation and expectation setting around the current business facts",
                "elasticsearch-hybrid",
                1,
                List.of("seed-order"),
                "COMPLETED",
                "REPLY_DRAFTED",
                "workflow completed",
                draftStatus,
                "LLM",
                null,
                sendReadiness,
                nextAction,
                1,
                latestDispatchStatus,
                null,
                manualReviewOutcome,
                List.of(),
                null,
                null,
                0,
                0,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                riskDecision,
                OffsetDateTime.now()
        );
    }

    private WorkflowRiskDecisionView riskDecision(String riskLevel,
                                                  String releaseDecision,
                                                  boolean sendAllowed,
                                                  String recommendedAction) {
        return new WorkflowRiskDecisionView(
                riskLevel,
                releaseDecision,
                sendAllowed,
                recommendedAction,
                List.of(),
                List.of("signal")
        );
    }
}
