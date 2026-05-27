package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkflowRunService {
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final ReplyDispatchRepository replyDispatchRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final WorkflowEventRecorder workflowEventRecorder;
    private final WorkflowStageExecutor workflowStageExecutor;
    private final WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser;

    public WorkflowRunService(WorkflowRunRepository workflowRunRepository,
                              WorkflowEventRepository workflowEventRepository,
                              ReplyDraftRepository replyDraftRepository,
                              ReplyDispatchRepository replyDispatchRepository,
                              ReviewRecordRepository reviewRecordRepository,
                              WorkflowEventRecorder workflowEventRecorder,
                              WorkflowStageExecutor workflowStageExecutor,
                              WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.workflowEventRecorder = workflowEventRecorder;
        this.workflowStageExecutor = workflowStageExecutor;
        this.workflowEvidenceSummaryParser = workflowEvidenceSummaryParser;
    }

    public WorkflowRun start(InboundMail inboundMail) {
        WorkflowRun run = WorkflowRun.start(inboundMail.messageId(), inboundMail.threadId());
        workflowRunRepository.save(run);
        workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
        return workflowStageExecutor.execute(run, inboundMail);
    }

    public List<WorkflowRun> findAllRuns() {
        return workflowRunRepository.findAll();
    }

    public Optional<WorkflowRun> findRun(String runId) {
        return workflowRunRepository.findByRunId(runId);
    }

    public Optional<WorkflowReplayView> findReplay(String runId) {
        return workflowRunRepository.findByRunId(runId)
                .map(this::buildReplayView);
    }

    public Optional<WorkflowReplayView> findReplayByMessageId(String messageId) {
        return workflowRunRepository.findLatestByMessageId(messageId)
                .map(this::buildReplayView);
    }

    public List<WorkflowEvent> findEvents(String runId) {
        return workflowEventRepository.findByRunId(runId);
    }

    public Optional<ReplyDraft> findDraft(String runId) {
        return replyDraftRepository.findByRunId(runId);
    }

    private WorkflowReplayView buildReplayView(WorkflowRun run) {
        List<WorkflowEvent> events = workflowEventRepository.findByRunId(run.getRunId());
        ReplyDraft draft = replyDraftRepository.findByRunId(run.getRunId()).orElse(null);
        List<com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch> dispatches = replyDispatchRepository.findByRunId(run.getRunId());
        List<com.leak.intelligentcustomerchat.domain.review.ReviewRecord> reviews = reviewRecordRepository.findByRunId(run.getRunId());
        WorkflowReplayEvidenceView evidence = buildReplayEvidence(events);
        return new WorkflowReplayView(
                run,
                events,
                draft,
                evidence,
                buildRiskDecision(run, draft, dispatches, reviews, evidence),
                dispatches,
                reviews
        );
    }

    private WorkflowReplayEvidenceView buildReplayEvidence(List<WorkflowEvent> events) {
        String routingSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.INTENT_ROUTED)
                .orElse("intent route summary unavailable");
        String businessFactsSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.BUSINESS_FACTS_READY)
                .orElse("business facts summary unavailable");
        String knowledgeSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.KNOWLEDGE_READY)
                .orElse("knowledge summary unavailable");
        String replyDraftSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.REPLY_DRAFTED)
                .orElse("reply draft summary unavailable");

        var routingTokens = workflowEvidenceSummaryParser.parseTokens(routingSummary);
        var businessTokens = workflowEvidenceSummaryParser.parseTokens(businessFactsSummary);
        var knowledgeTokens = workflowEvidenceSummaryParser.parseTokens(knowledgeSummary);
        var replyTokens = workflowEvidenceSummaryParser.parseTokens(replyDraftSummary);
        String subIntent = workflowEvidenceSummaryParser.tokenOrDefault(routingTokens, "subIntent", "general_inquiry");
        return new WorkflowReplayEvidenceView(
                workflowEvidenceSummaryParser.tokenOrDefault(businessTokens, "factStatus", "UNKNOWN"),
                workflowEvidenceSummaryParser.summarizeBusinessFacts(subIntent, businessFactsSummary),
                workflowEvidenceSummaryParser.splitPipeList(businessTokens.get("sourceSystems")),
                workflowEvidenceSummaryParser.parseInt(businessTokens.get("factCount"), 0),
                workflowEvidenceSummaryParser.parseInt(businessTokens.get("missingEntityCount"), 0),
                workflowEvidenceSummaryParser.parseInt(businessTokens.get("conflictFlagCount"), 0),
                workflowEvidenceSummaryParser.summarizeKnowledgeRole(subIntent),
                workflowEvidenceSummaryParser.tokenOrDefault(knowledgeTokens, "retrievalSource", "unknown"),
                workflowEvidenceSummaryParser.parseInt(knowledgeTokens.get("knowledgeRecallCount"), 0),
                workflowEvidenceSummaryParser.splitPipeList(knowledgeTokens.get("snippetIds")),
                workflowEvidenceSummaryParser.tokenOrDefault(replyTokens, "replySource", "UNKNOWN").toUpperCase(java.util.Locale.ROOT),
                replyTokens.get("fallbackReason")
        );
    }

    private WorkflowRiskDecisionView buildRiskDecision(WorkflowRun run,
                                                       ReplyDraft draft,
                                                       List<com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch> dispatches,
                                                       List<com.leak.intelligentcustomerchat.domain.review.ReviewRecord> reviews,
                                                       WorkflowReplayEvidenceView evidence) {
        com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch latestDispatch = dispatches.isEmpty()
                ? null
                : dispatches.get(dispatches.size() - 1);
        com.leak.intelligentcustomerchat.domain.review.ReviewRecord latestReview = reviews.isEmpty()
                ? null
                : reviews.get(reviews.size() - 1);
        List<String> riskFlags = buildReplayRiskFlags(run, draft, latestDispatch, latestReview, reviews, evidence);
        return WorkflowRiskDecisionResolver.resolve(
                run.getStatus().name(),
                draft == null ? null : draft.getStatus().name(),
                draft == null ? null : draft.getSendReadiness().name(),
                draft == null ? null : draft.getNextAction(),
                latestDispatch == null ? null : latestDispatch.getStatus().name(),
                latestReview == null ? null : latestReview.getAction().name(),
                determineManualReviewOutcome(draft, reviews),
                riskFlags
        );
    }

    private List<String> buildReplayRiskFlags(WorkflowRun run,
                                              ReplyDraft draft,
                                              com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch latestDispatch,
                                              com.leak.intelligentcustomerchat.domain.review.ReviewRecord latestReview,
                                              List<com.leak.intelligentcustomerchat.domain.review.ReviewRecord> reviews,
                                              WorkflowReplayEvidenceView evidence) {
        java.util.ArrayList<String> riskFlags = new java.util.ArrayList<>();
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
        if (reviews.stream().anyMatch(review -> review.getAction() == com.leak.intelligentcustomerchat.domain.review.ReviewAction.REVISE_DRAFT)) {
            riskFlags.add("draft_revised");
        }
        if (reviews.stream().anyMatch(review -> review.getAction() == com.leak.intelligentcustomerchat.domain.review.ReviewAction.RESUBMIT_REVIEW)) {
            riskFlags.add("review_resubmitted");
        }
        if ("TEMPLATE".equals(evidence.replySource())) {
            riskFlags.add("reply_template_fallback");
        }
        if ("FOLLOW-UP-TEMPLATE".equals(evidence.replySource())) {
            riskFlags.add("reply_follow_up_template");
        }
        if ("HUMAN-REVIEW-TEMPLATE".equals(evidence.replySource())) {
            riskFlags.add("reply_human_review_template");
        }
        if (evidence.replyFallbackReason() != null && !evidence.replyFallbackReason().isBlank()) {
            riskFlags.add("reply_fallback_recorded");
        }
        if ("CONFLICT".equals(evidence.businessFactStatus())) {
            riskFlags.add("business_fact_conflict");
        }
        if ("INSUFFICIENT_INPUT".equals(evidence.businessFactStatus())) {
            riskFlags.add("business_fact_insufficient_input");
        }
        return List.copyOf(riskFlags);
    }

    private String determineManualReviewOutcome(ReplyDraft draft,
                                                List<com.leak.intelligentcustomerchat.domain.review.ReviewRecord> reviews) {
        if (reviews.isEmpty()) {
            return "NOT_REVIEWED";
        }
        com.leak.intelligentcustomerchat.domain.review.ReviewAction latestAction = reviews.get(reviews.size() - 1).getAction();
        return switch (latestAction) {
            case APPROVE_SEND -> "APPROVED_FOR_SEND";
            case REJECT_SEND -> "REJECTED_FOR_REVISION";
            case RESUBMIT_REVIEW -> "RESUBMITTED_PENDING_REVIEW";
            case REVISE_DRAFT -> {
                if (draft != null && draft.getSendReadiness() == com.leak.intelligentcustomerchat.domain.reply.SendReadiness.PENDING_REVIEW) {
                    yield "REVISED_PENDING_REVIEW";
                }
                yield "REVISED_DRAFT";
            }
        };
    }

    private Optional<String> findEventSummary(List<WorkflowEvent> events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage stage) {
        return events.stream()
                .filter(event -> event.stage() == stage)
                .reduce((first, second) -> second)
                .map(WorkflowEvent::summary);
    }
}
