package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class WorkflowEvaluationService {
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final ReplyDispatchRepository replyDispatchRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final MailReceiptRepository mailReceiptRepository;

    public WorkflowEvaluationService(WorkflowRunRepository workflowRunRepository,
                                     WorkflowEventRepository workflowEventRepository,
                                     ReplyDraftRepository replyDraftRepository,
                                     ReplyDispatchRepository replyDispatchRepository,
                                     ReviewRecordRepository reviewRecordRepository,
                                     MailReceiptRepository mailReceiptRepository) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.mailReceiptRepository = mailReceiptRepository;
    }

    public WorkflowEvaluationSampleView getSample(String runId) {
        WorkflowRun run = workflowRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("workflow run not found for runId=" + runId));
        return buildSample(run);
    }

    public List<WorkflowEvaluationSampleView> listRecentSamples(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        return workflowRunRepository.findAll().stream()
                .limit(limit)
                .map(this::buildSample)
                .toList();
    }

    private WorkflowEvaluationSampleView buildSample(WorkflowRun run) {
        List<WorkflowEvent> events = workflowEventRepository.findByRunId(run.getRunId());
        ReplyDraft draft = replyDraftRepository.findByRunId(run.getRunId()).orElse(null);
        List<ReplyDispatch> dispatches = replyDispatchRepository.findByRunId(run.getRunId());
        List<ReviewRecord> reviews = reviewRecordRepository.findByRunId(run.getRunId());
        MailReceipt receipt = mailReceiptRepository.findByMessageId(run.getMessageId())
                .orElseGet(() -> fallbackReceipt(run));

        ReplyDispatch latestDispatch = dispatches.isEmpty() ? null : dispatches.get(dispatches.size() - 1);
        ReviewRecord latestReview = reviews.isEmpty() ? null : reviews.get(reviews.size() - 1);

        String normalizationSummary = findEventSummary(events, WorkflowStage.INTENT_NORMALIZED)
                .orElse("intent normalization summary unavailable");
        String routingSummary = findEventSummary(events, WorkflowStage.INTENT_ROUTED)
                .orElse("intent route summary unavailable");
        String businessFactsSummary = findEventSummary(events, WorkflowStage.BUSINESS_FACTS_READY)
                .orElse("business facts summary unavailable");
        String knowledgeSummary = findEventSummary(events, WorkflowStage.KNOWLEDGE_READY)
                .orElse("knowledge summary unavailable");

        return new WorkflowEvaluationSampleView(
                run.getRunId(),
                run.getMessageId(),
                run.getThreadId(),
                receipt.getSender(),
                receipt.getSubject(),
                normalizationSummary,
                routingSummary,
                containsStage(events, WorkflowStage.BUSINESS_FACTS_READY),
                businessFactsSummary,
                containsStage(events, WorkflowStage.KNOWLEDGE_READY),
                knowledgeSummary,
                run.getStatus().name(),
                run.getStage().name(),
                run.getStatusReason(),
                draft == null ? null : draft.getStatus().name(),
                draft == null ? null : draft.getSendReadiness().name(),
                draft == null ? null : draft.getNextAction(),
                draft == null ? null : draft.getDraftVersion(),
                latestDispatch == null ? null : latestDispatch.getStatus().name(),
                latestReview == null ? null : latestReview.getAction().name(),
                latestReview == null ? null : latestReview.getReviewer(),
                latestReview == null ? null : latestReview.getReviewNote(),
                buildRiskFlags(run, draft, latestDispatch, latestReview, businessFactsSummary),
                OffsetDateTime.now()
        );
    }

    private boolean containsStage(List<WorkflowEvent> events, WorkflowStage stage) {
        return events.stream().anyMatch(event -> event.stage() == stage);
    }

    private Optional<String> findEventSummary(List<WorkflowEvent> events, WorkflowStage stage) {
        return events.stream()
                .filter(event -> event.stage() == stage)
                .reduce((first, second) -> second)
                .map(WorkflowEvent::summary);
    }

    private List<String> buildRiskFlags(WorkflowRun run,
                                        ReplyDraft draft,
                                        ReplyDispatch latestDispatch,
                                        ReviewRecord latestReview,
                                        String businessFactsSummary) {
        List<String> riskFlags = new ArrayList<>();
        if ("BLOCKED".equals(run.getStatus().name())) {
            riskFlags.add("workflow_blocked");
        }
        if (draft != null && draft.isHumanReviewRequired()) {
            riskFlags.add("manual_review_required");
        }
        if (draft != null && draft.isFollowUpNeeded()) {
            riskFlags.add("follow_up_needed");
        }
        if (latestDispatch != null && latestDispatch.isRetryPending()) {
            riskFlags.add("dispatch_retry_pending");
        }
        if (latestDispatch != null && latestDispatch.isFailedFinal()) {
            riskFlags.add("dispatch_failed_final");
        }
        if (latestReview != null && "REJECT_SEND".equals(latestReview.getAction().name())) {
            riskFlags.add("review_rejected");
        }
        if (businessFactsSummary.contains("CONFLICT")) {
            riskFlags.add("business_fact_conflict");
        }
        if (businessFactsSummary.contains("INSUFFICIENT_INPUT")) {
            riskFlags.add("business_fact_insufficient_input");
        }
        return riskFlags;
    }

    private MailReceipt fallbackReceipt(WorkflowRun run) {
        return MailReceipt.manual("evaluation-fallback-" + run.getRunId(), new com.leak.intelligentcustomerchat.domain.mail.InboundMail(
                run.getMessageId(),
                run.getThreadId(),
                "unknown@example.com",
                "unknown subject",
                "evaluation fallback body",
                run.getCreatedAt()
        ));
    }
}
