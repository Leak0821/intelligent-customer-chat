package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WorkflowCaseView(
        String runId,
        String messageId,
        String threadId,
        String sender,
        String subject,
        String scene,
        String subIntent,
        String workflowStatus,
        String workflowStage,
        String workflowReason,
        String draftStatus,
        String sendReadiness,
        String nextAction,
        String latestDispatchStatus,
        String latestReviewAction,
        String manualReviewOutcome,
        WorkflowRiskDecisionView riskDecision,
        List<String> availableActions,
        String summaryMessage,
        String normalizationSummary,
        String businessFactsSummary,
        String knowledgeSummary,
        String draftSubject,
        String draftBody,
        Integer draftVersion,
        OffsetDateTime updatedAt
) {
    public WorkflowCaseView {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(threadId, "threadId must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(subIntent, "subIntent must not be null");
        Objects.requireNonNull(workflowStatus, "workflowStatus must not be null");
        Objects.requireNonNull(workflowStage, "workflowStage must not be null");
        Objects.requireNonNull(workflowReason, "workflowReason must not be null");
        Objects.requireNonNull(manualReviewOutcome, "manualReviewOutcome must not be null");
        Objects.requireNonNull(riskDecision, "riskDecision must not be null");
        Objects.requireNonNull(availableActions, "availableActions must not be null");
        Objects.requireNonNull(summaryMessage, "summaryMessage must not be null");
        Objects.requireNonNull(normalizationSummary, "normalizationSummary must not be null");
        Objects.requireNonNull(businessFactsSummary, "businessFactsSummary must not be null");
        Objects.requireNonNull(knowledgeSummary, "knowledgeSummary must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        availableActions = List.copyOf(availableActions);
    }

    public static WorkflowCaseView from(WorkflowEvaluationSampleView evaluation, ReplyDraft latestDraft) {
        return new WorkflowCaseView(
                evaluation.runId(),
                evaluation.messageId(),
                evaluation.threadId(),
                evaluation.sender(),
                evaluation.subject(),
                evaluation.scene(),
                evaluation.subIntent(),
                evaluation.workflowStatus(),
                evaluation.workflowStage(),
                evaluation.workflowReason(),
                evaluation.draftStatus(),
                evaluation.sendReadiness(),
                evaluation.nextAction(),
                evaluation.latestDispatchStatus(),
                evaluation.latestReviewAction(),
                evaluation.manualReviewOutcome(),
                evaluation.riskDecision(),
                determineAvailableActions(evaluation),
                buildSummaryMessage(evaluation),
                evaluation.normalizationSummary(),
                evaluation.businessFactsSummary(),
                evaluation.knowledgeSummary(),
                latestDraft == null ? null : latestDraft.getSubject(),
                latestDraft == null ? null : latestDraft.getBody(),
                latestDraft == null ? null : latestDraft.getDraftVersion(),
                resolveUpdatedAt(evaluation, latestDraft)
        );
    }

    private static List<String> determineAvailableActions(WorkflowEvaluationSampleView evaluation) {
        List<String> actions = new ArrayList<>();
        if ("PENDING_REVIEW".equals(evaluation.sendReadiness())) {
            actions.add("APPROVE_SEND");
            actions.add("REJECT_SEND");
            actions.add("REVISE_DRAFT");
        }
        if ("READY_FOR_SEND".equals(evaluation.sendReadiness()) && evaluation.riskDecision().sendAllowed()) {
            actions.add("DISPATCH");
        }
        if ("RETRY_PENDING".equals(evaluation.latestDispatchStatus())) {
            actions.add("RETRY_DISPATCH");
        }
        if ("REJECTED_FOR_REVISION".equals(evaluation.manualReviewOutcome())) {
            actions.add("REVISE_DRAFT");
        }
        return List.copyOf(actions);
    }

    private static String buildSummaryMessage(WorkflowEvaluationSampleView evaluation) {
        if ("BLOCKED".equals(evaluation.workflowStatus())) {
            return "当前流程已阻断，需先排查阻断原因后再继续处理。";
        }
        if ("RETRY_PENDING".equals(evaluation.latestDispatchStatus())) {
            return "邮件发送失败，系统已进入待重试状态，可人工触发重试。";
        }
        if ("FAILED_FINAL".equals(evaluation.latestDispatchStatus())) {
            return "邮件发送失败且已达到重试上限，需要人工介入处理。";
        }
        if ("SENT".equals(evaluation.latestDispatchStatus())) {
            return "当前邮件回复已完成发送，闭环已走通。";
        }
        if ("FOLLOW_UP_NEEDED".equals(evaluation.draftStatus())) {
            return "系统已生成追问草稿，当前等待客户补充关键信息。";
        }
        if ("PENDING_REVIEW".equals(evaluation.sendReadiness())) {
            return "系统已生成回复草稿，当前等待人工审核。";
        }
        if ("READY_FOR_SEND".equals(evaluation.sendReadiness()) && evaluation.riskDecision().sendAllowed()) {
            return "系统已完成审核放行，当前可以执行发送。";
        }
        if ("HUMAN_REVIEW_REQUIRED".equals(evaluation.draftStatus())) {
            return "当前回复需要人工介入审核或改稿后再继续。";
        }
        return "系统已完成当前轮次处理，可继续查看草稿与风险结论。";
    }

    private static OffsetDateTime resolveUpdatedAt(WorkflowEvaluationSampleView evaluation, ReplyDraft latestDraft) {
        if (latestDraft != null && latestDraft.getUpdatedAt().isAfter(evaluation.sampledAt())) {
            return latestDraft.getUpdatedAt();
        }
        return evaluation.sampledAt();
    }
}
