package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewAction;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import com.leak.intelligentcustomerchat.app.review.ReviewFeedbackTagger;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser;
    private final ReviewFeedbackTagger reviewFeedbackTagger;

    public WorkflowEvaluationService(WorkflowRunRepository workflowRunRepository,
                                     WorkflowEventRepository workflowEventRepository,
                                     ReplyDraftRepository replyDraftRepository,
                                     ReplyDispatchRepository replyDispatchRepository,
                                     ReviewRecordRepository reviewRecordRepository,
                                     MailReceiptRepository mailReceiptRepository,
                                     WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser,
                                     ReviewFeedbackTagger reviewFeedbackTagger) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.mailReceiptRepository = mailReceiptRepository;
        this.workflowEvidenceSummaryParser = workflowEvidenceSummaryParser;
        this.reviewFeedbackTagger = reviewFeedbackTagger;
    }

    public WorkflowEvaluationSampleView getSample(String runId) {
        WorkflowRun run = workflowRunRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("workflow run not found for runId=" + runId));
        return buildSample(run);
    }

    public List<WorkflowEvaluationSampleView> listRecentSamples(int limit) {
        return listSamples(limit, null, null, null, null, null);
    }

    public WorkflowEvaluationSummaryView summarizeRecentSamples(int limit,
                                                                String scene,
                                                                String subIntent,
                                                                String workflowStatus,
                                                                String draftStatus,
                                                                String riskFlag) {
        return summarizeRecentSamples(limit, scene, subIntent, workflowStatus, draftStatus, riskFlag, null, null, null);
    }

    public WorkflowEvaluationSummaryView summarizeRecentSamples(int limit,
                                                                String scene,
                                                                String subIntent,
                                                                String workflowStatus,
                                                                String draftStatus,
                                                                String riskFlag,
                                                                String businessFactStatus,
                                                                String knowledgeRetrievalSource,
                                                                String replyFallbackReason) {
        return summarizeRecentSamples(
                limit,
                scene,
                subIntent,
                workflowStatus,
                draftStatus,
                riskFlag,
                businessFactStatus,
                null,
                null,
                knowledgeRetrievalSource,
                replyFallbackReason
        );
    }

    public WorkflowEvaluationSummaryView summarizeRecentSamples(int limit,
                                                                String scene,
                                                                String subIntent,
                                                                String workflowStatus,
                                                                String draftStatus,
                                                                String riskFlag,
                                                                String businessFactStatus,
                                                                String businessFactRole,
                                                                String knowledgeRole,
                                                                String knowledgeRetrievalSource,
                                                                String replyFallbackReason) {
        List<WorkflowEvaluationSampleView> samples = listSamples(
                limit,
                scene,
                subIntent,
                workflowStatus,
                draftStatus,
                riskFlag,
                businessFactStatus,
                businessFactRole,
                knowledgeRole,
                knowledgeRetrievalSource,
                replyFallbackReason
        );
        return new WorkflowEvaluationSummaryView(
                limit,
                samples.size(),
                summarize(samples, WorkflowEvaluationSampleView::scene),
                summarize(samples, WorkflowEvaluationSampleView::subIntent),
                summarize(samples, WorkflowEvaluationSampleView::workflowStatus),
                summarize(samples, WorkflowEvaluationSampleView::draftStatus),
                summarize(samples, WorkflowEvaluationSampleView::replySource),
                summarize(samples, WorkflowEvaluationSampleView::businessFactStatus),
                summarize(samples, WorkflowEvaluationSampleView::businessFactRole),
                summarize(samples, WorkflowEvaluationSampleView::knowledgeRole),
                summarize(samples, WorkflowEvaluationSampleView::knowledgeRetrievalSource),
                summarize(samples, WorkflowEvaluationSampleView::replyFallbackReason),
                summarize(samples, WorkflowEvaluationSampleView::latestReviewAction),
                summarize(samples, WorkflowEvaluationSampleView::manualReviewOutcome),
                summarizeExpandedCounts(samples, WorkflowEvaluationSampleView::reviewFeedbackTagCounts),
                summarizeRiskFlags(samples),
                OffsetDateTime.now()
        );
    }

    public List<WorkflowEvaluationSampleView> listSamples(int limit,
                                                          String scene,
                                                          String subIntent,
                                                          String workflowStatus,
                                                          String draftStatus,
                                                          String riskFlag) {
        return listSamples(limit, scene, subIntent, workflowStatus, draftStatus, riskFlag, null, null, null);
    }

    public List<WorkflowEvaluationSampleView> listSamples(int limit,
                                                          String scene,
                                                          String subIntent,
                                                          String workflowStatus,
                                                          String draftStatus,
                                                          String riskFlag,
                                                          String businessFactStatus,
                                                          String knowledgeRetrievalSource,
                                                          String replyFallbackReason) {
        return listSamples(
                limit,
                scene,
                subIntent,
                workflowStatus,
                draftStatus,
                riskFlag,
                businessFactStatus,
                null,
                null,
                knowledgeRetrievalSource,
                replyFallbackReason
        );
    }

    public List<WorkflowEvaluationSampleView> listSamples(int limit,
                                                          String scene,
                                                          String subIntent,
                                                          String workflowStatus,
                                                          String draftStatus,
                                                          String riskFlag,
                                                          String businessFactStatus,
                                                          String businessFactRole,
                                                          String knowledgeRole,
                                                          String knowledgeRetrievalSource,
                                                          String replyFallbackReason) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        return workflowRunRepository.findAll().stream()
                .limit(limit)
                .map(this::buildSample)
                .filter(sample -> matches(sample.scene(), scene))
                .filter(sample -> matches(sample.subIntent(), subIntent))
                .filter(sample -> matches(sample.workflowStatus(), workflowStatus))
                .filter(sample -> matches(sample.draftStatus(), draftStatus))
                .filter(sample -> matches(sample.businessFactStatus(), businessFactStatus))
                .filter(sample -> matches(sample.businessFactRole(), businessFactRole))
                .filter(sample -> matches(sample.knowledgeRole(), knowledgeRole))
                .filter(sample -> matches(sample.knowledgeRetrievalSource(), knowledgeRetrievalSource))
                .filter(sample -> matches(sample.replyFallbackReason(), replyFallbackReason))
                .filter(sample -> matchesRiskFlag(sample.riskFlags(), riskFlag))
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
        int reviewCount = reviews.size();
        int revisionCount = countAction(reviews, ReviewAction.REVISE_DRAFT);
        boolean resubmittedForReview = containsAction(reviews, ReviewAction.RESUBMIT_REVIEW);
        String manualReviewOutcome = determineManualReviewOutcome(draft, reviews);
        List<String> latestReviewFeedbackTags = latestReview == null
                ? List.of()
                : reviewFeedbackTagger.tagManualReviewNote(latestReview.getReviewNote());

        String normalizationSummary = findEventSummary(events, WorkflowStage.INTENT_NORMALIZED)
                .orElse("intent normalization summary unavailable");
        String routingSummary = findEventSummary(events, WorkflowStage.INTENT_ROUTED)
                .orElse("intent route summary unavailable");
        String businessFactsSummary = findEventSummary(events, WorkflowStage.BUSINESS_FACTS_READY)
                .orElse("business facts summary unavailable");
        String knowledgeSummary = findEventSummary(events, WorkflowStage.KNOWLEDGE_READY)
                .orElse("knowledge summary unavailable");
        String replyDraftSummary = findEventSummary(events, WorkflowStage.REPLY_DRAFTED)
                .orElse("reply draft summary unavailable");
        String scene = extractToken(routingSummary, "scene");
        String subIntent = extractToken(routingSummary, "subIntent");
        String replySource = extractToken(replyDraftSummary, "replySource");
        String replyFallbackReason = extractOptionalToken(replyDraftSummary, "fallbackReason");
        Map<String, String> businessTokens = workflowEvidenceSummaryParser.parseTokens(businessFactsSummary);
        Map<String, String> knowledgeTokens = workflowEvidenceSummaryParser.parseTokens(knowledgeSummary);
        String businessFactStatus = workflowEvidenceSummaryParser.tokenOrDefault(businessTokens, "factStatus", "UNKNOWN");

        return new WorkflowEvaluationSampleView(
                run.getRunId(),
                run.getMessageId(),
                run.getThreadId(),
                receipt.getSender(),
                receipt.getSubject(),
                normalizationSummary,
                scene,
                subIntent,
                routingSummary,
                containsStage(events, WorkflowStage.BUSINESS_FACTS_READY),
                businessFactsSummary,
                businessFactStatus,
                workflowEvidenceSummaryParser.summarizeBusinessFacts(subIntent, businessFactsSummary),
                workflowEvidenceSummaryParser.splitPipeList(businessTokens.get("sourceSystems")),
                containsStage(events, WorkflowStage.KNOWLEDGE_READY),
                knowledgeSummary,
                workflowEvidenceSummaryParser.summarizeKnowledgeRole(subIntent),
                workflowEvidenceSummaryParser.tokenOrDefault(knowledgeTokens, "retrievalSource", "unknown"),
                workflowEvidenceSummaryParser.parseInt(knowledgeTokens.get("knowledgeRecallCount"), 0),
                workflowEvidenceSummaryParser.splitPipeList(knowledgeTokens.get("snippetIds")),
                run.getStatus().name(),
                run.getStage().name(),
                run.getStatusReason(),
                draft == null ? null : draft.getStatus().name(),
                replySource,
                replyFallbackReason,
                draft == null ? null : draft.getSendReadiness().name(),
                draft == null ? null : draft.getNextAction(),
                draft == null ? null : draft.getDraftVersion(),
                latestDispatch == null ? null : latestDispatch.getStatus().name(),
                latestReview == null ? null : latestReview.getAction().name(),
                manualReviewOutcome,
                latestReviewFeedbackTags,
                latestReview == null ? null : latestReview.getReviewer(),
                latestReview == null ? null : latestReview.getReviewNote(),
                reviewCount,
                revisionCount,
                resubmittedForReview,
                summarizeReviewActions(reviews),
                summarizeReviewFeedbackTags(reviews),
                buildReviewTimeline(reviews),
                buildRiskFlags(run, draft, latestDispatch, latestReview, reviews, businessFactsSummary, replySource, replyFallbackReason),
                OffsetDateTime.now()
        );
    }

    private boolean matches(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return actual.equalsIgnoreCase(expected.trim());
    }

    private boolean matchesRiskFlag(List<String> riskFlags, String expectedRiskFlag) {
        if (expectedRiskFlag == null || expectedRiskFlag.isBlank()) {
            return true;
        }
        return riskFlags.stream().anyMatch(flag -> flag.equalsIgnoreCase(expectedRiskFlag.trim()));
    }

    private boolean containsStage(List<WorkflowEvent> events, WorkflowStage stage) {
        return events.stream().anyMatch(event -> event.stage() == stage);
    }

    private List<WorkflowEvaluationCountView> summarize(List<WorkflowEvaluationSampleView> samples,
                                                        java.util.function.Function<WorkflowEvaluationSampleView, String> extractor) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (WorkflowEvaluationSampleView sample : samples) {
            String key = extractor.apply(sample);
            if (key == null || key.isBlank()) {
                key = "UNKNOWN";
            }
            counts.merge(key, 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new WorkflowEvaluationCountView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<WorkflowEvaluationCountView> summarizeRiskFlags(List<WorkflowEvaluationSampleView> samples) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (WorkflowEvaluationSampleView sample : samples) {
            for (String riskFlag : sample.riskFlags()) {
                counts.merge(riskFlag, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new WorkflowEvaluationCountView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<WorkflowEvaluationCountView> summarizeExpandedCounts(List<WorkflowEvaluationSampleView> samples,
                                                                      java.util.function.Function<WorkflowEvaluationSampleView, List<WorkflowEvaluationCountView>> extractor) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (WorkflowEvaluationSampleView sample : samples) {
            for (WorkflowEvaluationCountView countView : extractor.apply(sample)) {
                counts.merge(countView.key(), countView.count(), Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new WorkflowEvaluationCountView(entry.getKey(), entry.getValue()))
                .toList();
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
                                        List<ReviewRecord> reviews,
                                        String businessFactsSummary,
                                        String replySource,
                                        String replyFallbackReason) {
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
        if (containsAction(reviews, ReviewAction.REVISE_DRAFT)) {
            riskFlags.add("draft_revised");
        }
        if (containsAction(reviews, ReviewAction.RESUBMIT_REVIEW)) {
            riskFlags.add("review_resubmitted");
        }
        if ("TEMPLATE".equals(replySource)) {
            riskFlags.add("reply_template_fallback");
        }
        if ("FOLLOW-UP-TEMPLATE".equals(replySource)) {
            riskFlags.add("reply_follow_up_template");
        }
        if ("HUMAN-REVIEW-TEMPLATE".equals(replySource)) {
            riskFlags.add("reply_human_review_template");
        }
        if (replyFallbackReason != null && !replyFallbackReason.isBlank()) {
            riskFlags.add("reply_fallback_recorded");
        }
        if (businessFactsSummary.contains("CONFLICT")) {
            riskFlags.add("business_fact_conflict");
        }
        if (businessFactsSummary.contains("INSUFFICIENT_INPUT")) {
            riskFlags.add("business_fact_insufficient_input");
        }
        return riskFlags;
    }

    private String extractToken(String summary, String key) {
        String marker = key + "=";
        int start = summary.indexOf(marker);
        if (start < 0) {
            return "UNKNOWN";
        }
        int valueStart = start + marker.length();
        int end = summary.indexOf(",", valueStart);
        String value = end < 0 ? summary.substring(valueStart) : summary.substring(valueStart, end);
        value = value.trim();
        if (value.isBlank()) {
            return "UNKNOWN";
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String extractOptionalToken(String summary, String key) {
        String marker = key + "=";
        int start = summary.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + marker.length();
        int end = summary.indexOf(",", valueStart);
        String value = end < 0 ? summary.substring(valueStart) : summary.substring(valueStart, end);
        value = value.trim();
        return value.isBlank() ? null : value;
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

    private int countAction(List<ReviewRecord> reviews, ReviewAction action) {
        return (int) reviews.stream()
                .filter(review -> review.getAction() == action)
                .count();
    }

    private boolean containsAction(List<ReviewRecord> reviews, ReviewAction action) {
        return reviews.stream().anyMatch(review -> review.getAction() == action);
    }

    private String determineManualReviewOutcome(ReplyDraft draft, List<ReviewRecord> reviews) {
        if (reviews.isEmpty()) {
            return "NOT_REVIEWED";
        }
        ReviewAction latestAction = reviews.get(reviews.size() - 1).getAction();
        return switch (latestAction) {
            case APPROVE_SEND -> "APPROVED_FOR_SEND";
            case REJECT_SEND -> "REJECTED_FOR_REVISION";
            case RESUBMIT_REVIEW -> "RESUBMITTED_PENDING_REVIEW";
            case REVISE_DRAFT -> {
                if (draft != null && draft.getSendReadiness() == SendReadiness.PENDING_REVIEW) {
                    yield "REVISED_PENDING_REVIEW";
                }
                yield "REVISED_DRAFT";
            }
        };
    }

    private List<WorkflowEvaluationCountView> summarizeReviewActions(List<ReviewRecord> reviews) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReviewRecord review : reviews) {
            counts.merge(review.getAction().name(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new WorkflowEvaluationCountView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<WorkflowEvaluationCountView> summarizeReviewFeedbackTags(List<ReviewRecord> reviews) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReviewRecord review : reviews) {
            for (String tag : reviewFeedbackTagger.tagManualReviewNote(review.getReviewNote())) {
                counts.merge(tag, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new WorkflowEvaluationCountView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<String> buildReviewTimeline(List<ReviewRecord> reviews) {
        return reviews.stream()
                .map(review -> "%s by %s: %s".formatted(
                        review.getAction().name(),
                        review.getReviewer(),
                        review.getReviewNote()
                ))
                .toList();
    }
}
