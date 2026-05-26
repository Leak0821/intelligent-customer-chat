package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import com.leak.intelligentcustomerchat.domain.reply.DispatchTriggerSource;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowQueueAdminServiceTest {

    @Test
    void shouldListReviewAndDispatchQueuesSeparately() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun reviewRun = WorkflowRun.start("msg-review", "thread-review");
        reviewRun.complete("draft ready and waiting review");
        workflowRunRepository.save(reviewRun);
        ReplyDraft reviewDraft = ReplyDraft.create(reviewRun.getRunId(), "Review subject", "Review body", ReplyDraftStatus.DRAFT_READY, "await review");
        reviewDraft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "await_review_decision", "await review");
        replyDraftRepository.save(reviewDraft);
        reviewRecordRepository.save(ReviewRecord.resubmitReview(reviewRun.getRunId(), reviewDraft.getDraftId(), "editor-a", "resubmitted for review"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-review", new InboundMail(
                reviewRun.getMessageId(),
                reviewRun.getThreadId(),
                "reviewer@example.com",
                "Need review",
                "body",
                OffsetDateTime.now()
        )));

        WorkflowRun dispatchRun = WorkflowRun.start("msg-dispatch", "thread-dispatch");
        dispatchRun.complete("approved and waiting dispatch");
        workflowRunRepository.save(dispatchRun);
        ReplyDraft dispatchDraft = ReplyDraft.create(dispatchRun.getRunId(), "Dispatch subject", "Dispatch body", ReplyDraftStatus.DRAFT_READY, "ready for send");
        dispatchDraft.updateSendReadiness(SendReadiness.READY_FOR_SEND, "dispatch_reply", "approved");
        replyDraftRepository.save(dispatchDraft);
        ReplyDispatch dispatch = ReplyDispatch.create(
                dispatchRun.getRunId(),
                dispatchDraft.getDraftId(),
                "customer@example.com",
                dispatchDraft.getSubject(),
                dispatchDraft.getBody(),
                3,
                DispatchTriggerSource.MANUAL_APPROVAL,
                "reviewer-b",
                "approved for send"
        );
        replyDispatchRepository.save(dispatch);
        reviewRecordRepository.save(ReviewRecord.approveSend(dispatchRun.getRunId(), dispatchDraft.getDraftId(), "reviewer-b", "approved for send"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-dispatch", new InboundMail(
                dispatchRun.getMessageId(),
                dispatchRun.getThreadId(),
                "dispatch@example.com",
                "Ready to dispatch",
                "body",
                OffsetDateTime.now()
        )));

        WorkflowQueueAdminService service = new WorkflowQueueAdminService(
                workflowRunRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository
        );

        List<WorkflowQueueItemView> reviewQueue = service.listReviewQueue(10);
        List<WorkflowQueueItemView> dispatchQueue = service.listDispatchQueue(10);

        assertThat(reviewQueue).hasSize(1);
        assertThat(reviewQueue.get(0).messageId()).isEqualTo("msg-review");
        assertThat(reviewQueue.get(0).sendReadiness()).isEqualTo("PENDING_REVIEW");
        assertThat(reviewQueue.get(0).latestReviewAction()).isEqualTo("RESUBMIT_REVIEW");

        assertThat(dispatchQueue).hasSize(1);
        assertThat(dispatchQueue.get(0).messageId()).isEqualTo("msg-dispatch");
        assertThat(dispatchQueue.get(0).sendReadiness()).isEqualTo("READY_FOR_SEND");
        assertThat(dispatchQueue.get(0).latestDispatchStatus()).isEqualTo("PENDING");
        assertThat(dispatchQueue.get(0).latestReviewAction()).isEqualTo("APPROVE_SEND");
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
        public long countByStatus(MailReceiptStatus status) {
            return receiptsByMessageId.values().stream()
                    .filter(receipt -> receipt.getStatus() == status)
                    .count();
        }
    }
}
