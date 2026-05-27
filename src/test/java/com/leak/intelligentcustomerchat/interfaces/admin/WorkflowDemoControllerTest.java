package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationSummaryView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowEvidenceSummaryParser;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowQueueAdminService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowQueueItemView;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.DispatchTriggerSource;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
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

class WorkflowDemoControllerTest {
    private final WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser = new WorkflowEvidenceSummaryParser();

    @Test
    void shouldExposeReviewAndDispatchQueues() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun reviewRun = WorkflowRun.start("msg-review", "thread-review");
        reviewRun.complete("awaiting review");
        workflowRunRepository.save(reviewRun);
        ReplyDraft reviewDraft = ReplyDraft.create(reviewRun.getRunId(), "Need review", "body-review", ReplyDraftStatus.DRAFT_READY, "await review");
        reviewDraft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "await_review_decision", "awaiting review");
        replyDraftRepository.save(reviewDraft);
        reviewRecordRepository.save(ReviewRecord.resubmitReview(reviewRun.getRunId(), reviewDraft.getDraftId(), "editor-a", "resubmitted"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-review", new InboundMail(
                reviewRun.getMessageId(),
                reviewRun.getThreadId(),
                "review@example.com",
                "Need review",
                "body-review",
                OffsetDateTime.now()
        )));

        WorkflowRun dispatchRun = WorkflowRun.start("msg-dispatch", "thread-dispatch");
        dispatchRun.complete("ready to dispatch");
        workflowRunRepository.save(dispatchRun);
        ReplyDraft dispatchDraft = ReplyDraft.create(dispatchRun.getRunId(), "Ready to dispatch", "body-dispatch", ReplyDraftStatus.DRAFT_READY, "ready");
        dispatchDraft.updateSendReadiness(SendReadiness.READY_FOR_SEND, "dispatch_reply", "approved");
        replyDraftRepository.save(dispatchDraft);
        replyDispatchRepository.save(ReplyDispatch.create(
                dispatchRun.getRunId(),
                dispatchDraft.getDraftId(),
                "dispatch@example.com",
                dispatchDraft.getSubject(),
                dispatchDraft.getBody(),
                3,
                DispatchTriggerSource.MANUAL_APPROVAL,
                "reviewer-a",
                "approved"
        ));
        reviewRecordRepository.save(ReviewRecord.approveSend(dispatchRun.getRunId(), dispatchDraft.getDraftId(), "reviewer-a", "approved"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-dispatch", new InboundMail(
                dispatchRun.getMessageId(),
                dispatchRun.getThreadId(),
                "dispatch@example.com",
                "Ready to dispatch",
                "body-dispatch",
                OffsetDateTime.now()
        )));

        WorkflowQueueAdminService workflowQueueAdminService = new WorkflowQueueAdminService(
                workflowRunRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository
        );

        WorkflowDemoController controller = new WorkflowDemoController(
                null,
                workflowQueueAdminService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<WorkflowQueueItemView> reviewQueue = controller.listReviewQueue(10);
        List<WorkflowQueueItemView> dispatchQueue = controller.listDispatchQueue(10);

        assertThat(reviewQueue).hasSize(1);
        assertThat(reviewQueue.get(0).runId()).isEqualTo(reviewRun.getRunId());
        assertThat(reviewQueue.get(0).sender()).isEqualTo("review@example.com");
        assertThat(reviewQueue.get(0).sendReadiness()).isEqualTo("PENDING_REVIEW");
        assertThat(dispatchQueue).hasSize(1);
        assertThat(dispatchQueue.get(0).runId()).isEqualTo(dispatchRun.getRunId());
        assertThat(dispatchQueue.get(0).sender()).isEqualTo("dispatch@example.com");
        assertThat(dispatchQueue.get(0).sendReadiness()).isEqualTo("READY_FOR_SEND");
    }

    @Test
    void shouldExposeEvaluationSummary() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryWorkflowEventRepository workflowEventRepository = new InMemoryWorkflowEventRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun run = WorkflowRun.start("msg-summary", "thread-summary");
        run.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=logistics_tracking");
        run.complete("workflow completed with draft status=FOLLOW_UP_NEEDED");
        workflowRunRepository.save(run);
        workflowEventRepository.save(new WorkflowEvent("evt-1", run.getRunId(), run.getMessageId(), WorkflowStage.INTENT_ROUTED, run.getStatus(), "scene=AFTER_SALES, subIntent=logistics_tracking", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-2", run.getRunId(), run.getMessageId(), WorkflowStage.REPLY_DRAFTED, run.getStatus(), "draftStatus=FOLLOW_UP_NEEDED, replySource=follow-up-template, fallbackReason=follow_up_template_required", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(run.getRunId(), "subject-summary", "body-summary", ReplyDraftStatus.FOLLOW_UP_NEEDED, "missing order id"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-summary", new InboundMail(
                run.getMessageId(),
                run.getThreadId(),
                "summary@example.com",
                "subject-summary",
                "body-summary",
                OffsetDateTime.now()
        )));

        WorkflowEvaluationService workflowEvaluationService = new WorkflowEvaluationService(
                workflowRunRepository,
                workflowEventRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository,
                workflowEvidenceSummaryParser
        );

        WorkflowDemoController controller = new WorkflowDemoController(
                null,
                null,
                null,
                null,
                null,
                workflowEvaluationService,
                null,
                null,
                null,
                null
        );

        WorkflowEvaluationSummaryView summary = controller.evaluationSummary(20, "AFTER_SALES", null, null, null, null);

        assertThat(summary.requestedLimit()).isEqualTo(20);
        assertThat(summary.sampledCount()).isEqualTo(1);
        assertThat(summary.scenes()).containsExactly(new com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationCountView("AFTER_SALES", 1));
        assertThat(summary.replySources()).containsExactly(new com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationCountView("FOLLOW-UP-TEMPLATE", 1));
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
        private final Map<String, List<ReviewRecord>> reviewsByRunId = new LinkedHashMap<>();

        @Override
        public ReviewRecord save(ReviewRecord reviewRecord) {
            reviewsByRunId.computeIfAbsent(reviewRecord.getRunId(), key -> new ArrayList<>()).add(reviewRecord);
            return reviewRecord;
        }

        @Override
        public List<ReviewRecord> findByRunId(String runId) {
            return reviewsByRunId.getOrDefault(runId, List.of());
        }

        @Override
        public Optional<ReviewRecord> findLatestApprovalByRunId(String runId) {
            return reviewsByRunId.getOrDefault(runId, List.of()).stream()
                    .filter(review -> "APPROVE_SEND".equals(review.getAction().name()))
                    .max(Comparator.comparing(ReviewRecord::getCreatedAt));
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
            return List.of();
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
        public long countByStatus(MailReceiptStatus status) {
            return receiptsByMessageId.values().stream()
                    .filter(receipt -> receipt.getStatus() == status)
                    .count();
        }
    }
}
