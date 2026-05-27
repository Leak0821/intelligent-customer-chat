package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.DispatchTriggerSource;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEvaluationServiceTest {

    @Test
    void shouldBuildEvaluationSampleFromWorkflowArtifacts() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryWorkflowEventRepository workflowEventRepository = new InMemoryWorkflowEventRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun run = WorkflowRun.start("msg-eval-1", "thread-eval-1");
        run.moveTo(WorkflowStage.INTENT_NORMALIZED, "disposition=CONTINUE, missing=[]");
        run.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=logistics_tracking");
        run.moveTo(WorkflowStage.BUSINESS_FACTS_READY, "factStatus=CONFLICT, facts=1");
        run.moveTo(WorkflowStage.KNOWLEDGE_READY, "knowledgeRecallCount=3");
        run.complete("workflow completed with draft status=HUMAN_REVIEW_REQUIRED");
        workflowRunRepository.save(run);

        workflowEventRepository.save(new WorkflowEvent("evt-1", run.getRunId(), run.getMessageId(), WorkflowStage.INTENT_NORMALIZED, run.getStatus(), "disposition=CONTINUE, missing=[]", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-2", run.getRunId(), run.getMessageId(), WorkflowStage.INTENT_ROUTED, run.getStatus(), "scene=AFTER_SALES, subIntent=logistics_tracking", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-3", run.getRunId(), run.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, run.getStatus(), "factStatus=CONFLICT, facts=1", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-4", run.getRunId(), run.getMessageId(), WorkflowStage.KNOWLEDGE_READY, run.getStatus(), "knowledgeRecallCount=3", OffsetDateTime.now()));

        ReplyDraft draft = ReplyDraft.create(run.getRunId(), "Re: Where is my order", "Please allow us to check.", ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, "manual review required");
        draft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "manual_review_required", "manual review required");
        replyDraftRepository.save(draft);

        ReplyDispatch dispatch = ReplyDispatch.create(
                run.getRunId(),
                draft.getDraftId(),
                "customer@example.com",
                draft.getSubject(),
                draft.getBody(),
                3,
                DispatchTriggerSource.MANUAL_APPROVAL,
                "auditor-a",
                "approved"
        );
        dispatch.markAttemptResult(false, null, "smtp timeout", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(5));
        replyDispatchRepository.save(dispatch);

        reviewRecordRepository.save(ReviewRecord.reviseDraft(run.getRunId(), draft.getDraftId(), "editor-a", "soften the compensation promise"));
        reviewRecordRepository.save(ReviewRecord.resubmitReview(run.getRunId(), draft.getDraftId(), "editor-a", "resubmitted after manual revision"));
        reviewRecordRepository.save(ReviewRecord.rejectSend(run.getRunId(), draft.getDraftId(), "auditor-b", "promise wording needs revision"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-eval-1", new InboundMail(
                run.getMessageId(),
                run.getThreadId(),
                "customer@example.com",
                "Where is my order",
                "Please help check my order.",
                OffsetDateTime.now()
        )));

        WorkflowEvaluationService service = new WorkflowEvaluationService(
                workflowRunRepository,
                workflowEventRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository
        );

        WorkflowEvaluationSampleView sample = service.getSample(run.getRunId());

        assertThat(sample.runId()).isEqualTo(run.getRunId());
        assertThat(sample.subject()).isEqualTo("Where is my order");
        assertThat(sample.routingSummary()).contains("AFTER_SALES");
        assertThat(sample.businessFactsTriggered()).isTrue();
        assertThat(sample.knowledgeTriggered()).isTrue();
        assertThat(sample.draftStatus()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        assertThat(sample.latestDispatchStatus()).isEqualTo("RETRY_PENDING");
        assertThat(sample.latestReviewAction()).isEqualTo("REJECT_SEND");
        assertThat(sample.reviewCount()).isEqualTo(3);
        assertThat(sample.revisionCount()).isEqualTo(1);
        assertThat(sample.resubmittedForReview()).isTrue();
        assertThat(sample.reviewTimeline()).contains(
                "REVISE_DRAFT by editor-a: soften the compensation promise",
                "RESUBMIT_REVIEW by editor-a: resubmitted after manual revision",
                "REJECT_SEND by auditor-b: promise wording needs revision"
        );
        assertThat(sample.riskFlags()).contains(
                "manual_review_required",
                "dispatch_retry_pending",
                "review_rejected",
                "business_fact_conflict",
                "draft_revised",
                "review_resubmitted"
        );
        assertThat(sample.scene()).isEqualTo("AFTER_SALES");
        assertThat(sample.subIntent()).isEqualTo("LOGISTICS_TRACKING");
    }

    @Test
    void shouldFilterSamplesBySceneStatusAndRiskFlag() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryWorkflowEventRepository workflowEventRepository = new InMemoryWorkflowEventRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun afterSalesRun = WorkflowRun.start("msg-eval-2", "thread-eval-2");
        afterSalesRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=logistics_tracking");
        afterSalesRun.complete("workflow completed with draft status=FOLLOW_UP_NEEDED");
        workflowRunRepository.save(afterSalesRun);
        workflowEventRepository.save(new WorkflowEvent("evt-a", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.INTENT_ROUTED, afterSalesRun.getStatus(), "scene=AFTER_SALES, subIntent=logistics_tracking", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(afterSalesRun.getRunId(), "subject-a", "body-a", ReplyDraftStatus.FOLLOW_UP_NEEDED, "missing order id"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-a", new InboundMail(afterSalesRun.getMessageId(), afterSalesRun.getThreadId(), "a@example.com", "subject-a", "body-a", OffsetDateTime.now())));

        WorkflowRun preSalesRun = WorkflowRun.start("msg-eval-3", "thread-eval-3");
        preSalesRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=PRE_SALES, subIntent=product_recommendation");
        preSalesRun.complete("workflow completed with draft status=DRAFT_READY");
        workflowRunRepository.save(preSalesRun);
        workflowEventRepository.save(new WorkflowEvent("evt-b", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.INTENT_ROUTED, preSalesRun.getStatus(), "scene=PRE_SALES, subIntent=product_recommendation", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(preSalesRun.getRunId(), "subject-b", "body-b", ReplyDraftStatus.DRAFT_READY, "ready"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-b", new InboundMail(preSalesRun.getMessageId(), preSalesRun.getThreadId(), "b@example.com", "subject-b", "body-b", OffsetDateTime.now())));

        WorkflowEvaluationService service = new WorkflowEvaluationService(
                workflowRunRepository,
                workflowEventRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository
        );

        List<WorkflowEvaluationSampleView> afterSalesSamples = service.listSamples(10, "AFTER_SALES", null, null, null, null);
        List<WorkflowEvaluationSampleView> followUpSamples = service.listSamples(10, null, null, null, "FOLLOW_UP_NEEDED", "follow_up_needed");

        assertThat(afterSalesSamples).hasSize(1);
        assertThat(afterSalesSamples.get(0).messageId()).isEqualTo("msg-eval-2");
        assertThat(followUpSamples).hasSize(1);
        assertThat(followUpSamples.get(0).scene()).isEqualTo("AFTER_SALES");
    }

    private static final class InMemoryWorkflowRunRepository implements WorkflowRunRepository {
        private final Map<String, WorkflowRun> runs = new LinkedHashMap<>();

        @Override
        public WorkflowRun save(WorkflowRun run) {
            runs.put(run.getRunId(), run);
            return run;
        }

        @Override
        public Optional<WorkflowRun> findByRunId(String runId) {
            return Optional.ofNullable(runs.get(runId));
        }

        @Override
        public Optional<WorkflowRun> findLatestByMessageId(String messageId) {
            return runs.values().stream().filter(run -> run.getMessageId().equals(messageId)).findFirst();
        }

        @Override
        public List<WorkflowRun> findAll() {
            return runs.values().stream()
                    .sorted(Comparator.comparing(WorkflowRun::getCreatedAt).reversed())
                    .toList();
        }
    }

    private static final class InMemoryWorkflowEventRepository implements WorkflowEventRepository {
        private final List<WorkflowEvent> events = new ArrayList<>();

        @Override
        public void save(WorkflowEvent event) {
            events.add(event);
        }

        @Override
        public List<WorkflowEvent> findByRunId(String runId) {
            return events.stream().filter(event -> event.runId().equals(runId)).toList();
        }

        @Override
        public List<WorkflowEvent> findAll() {
            return List.copyOf(events);
        }
    }

    private static final class InMemoryReplyDraftRepository implements ReplyDraftRepository {
        private final Map<String, ReplyDraft> draftsByRunId = new LinkedHashMap<>();

        @Override
        public ReplyDraft save(ReplyDraft draft) {
            draftsByRunId.put(draft.getRunId(), draft);
            return draft;
        }

        @Override
        public Optional<ReplyDraft> findByRunId(String runId) {
            return Optional.ofNullable(draftsByRunId.get(runId));
        }
    }

    private static final class InMemoryReplyDispatchRepository implements ReplyDispatchRepository {
        private final Map<String, List<ReplyDispatch>> dispatchesByRunId = new LinkedHashMap<>();

        @Override
        public ReplyDispatch save(ReplyDispatch dispatch) {
            dispatchesByRunId.computeIfAbsent(dispatch.getRunId(), key -> new ArrayList<>()).add(dispatch);
            return dispatch;
        }

        @Override
        public List<ReplyDispatch> findByRunId(String runId) {
            return dispatchesByRunId.getOrDefault(runId, List.of());
        }

        @Override
        public Optional<ReplyDispatch> findLatestByRunId(String runId) {
            List<ReplyDispatch> dispatches = dispatchesByRunId.get(runId);
            if (dispatches == null || dispatches.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(dispatches.get(dispatches.size() - 1));
        }

        @Override
        public List<ReplyDispatch> findRetryableDueBefore(OffsetDateTime dueBefore, int limit) {
            return List.of();
        }
    }

    private static final class InMemoryReviewRecordRepository implements ReviewRecordRepository {
        private final List<ReviewRecord> reviewRecords = new ArrayList<>();

        @Override
        public ReviewRecord save(ReviewRecord reviewRecord) {
            reviewRecords.add(reviewRecord);
            return reviewRecord;
        }

        @Override
        public List<ReviewRecord> findByRunId(String runId) {
            return reviewRecords.stream().filter(record -> record.getRunId().equals(runId)).toList();
        }

        @Override
        public Optional<ReviewRecord> findLatestApprovalByRunId(String runId) {
            return reviewRecords.stream()
                    .filter(record -> record.getRunId().equals(runId) && record.isApproval())
                    .reduce((first, second) -> second);
        }
    }

    private static final class InMemoryMailReceiptRepository implements MailReceiptRepository {
        private final Map<String, MailReceipt> receiptsByMessageId = new LinkedHashMap<>();

        @Override
        public boolean existsBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
            return false;
        }

        @Override
        public MailReceipt save(MailReceipt receipt) {
            receiptsByMessageId.put(receipt.getMessageId(), receipt);
            return receipt;
        }

        @Override
        public Optional<MailReceipt> findBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
            return Optional.empty();
        }

        @Override
        public Optional<MailReceipt> findByMessageId(String messageId) {
            return Optional.ofNullable(receiptsByMessageId.get(messageId));
        }

        @Override
        public List<MailReceipt> findPendingForProcessing(int limit) {
            return receiptsByMessageId.values().stream().limit(limit).toList();
        }

        @Override
        public List<MailReceipt> findRecent(int limit) {
            return receiptsByMessageId.values().stream().limit(limit).toList();
        }

        @Override
        public long countAll() {
            return receiptsByMessageId.size();
        }

        @Override
        public long countByStatus(com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus status) {
            return receiptsByMessageId.values().stream()
                    .filter(receipt -> receipt.getStatus() == status)
                    .count();
        }
    }
}
