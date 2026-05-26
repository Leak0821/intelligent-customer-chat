package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class WorkflowQueueAdminService {
    private final WorkflowRunRepository workflowRunRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final ReplyDispatchRepository replyDispatchRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final MailReceiptRepository mailReceiptRepository;

    public WorkflowQueueAdminService(WorkflowRunRepository workflowRunRepository,
                                     ReplyDraftRepository replyDraftRepository,
                                     ReplyDispatchRepository replyDispatchRepository,
                                     ReviewRecordRepository reviewRecordRepository,
                                     MailReceiptRepository mailReceiptRepository) {
        this.workflowRunRepository = workflowRunRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.mailReceiptRepository = mailReceiptRepository;
    }

    public List<WorkflowQueueItemView> listReviewQueue(int limit) {
        return listQueue(limit, item -> "PENDING_REVIEW".equals(item.sendReadiness()));
    }

    public List<WorkflowQueueItemView> listDispatchQueue(int limit) {
        return listQueue(limit, item -> "READY_FOR_SEND".equals(item.sendReadiness()));
    }

    private List<WorkflowQueueItemView> listQueue(int limit, Predicate<WorkflowQueueItemView> predicate) {
        int normalizedLimit = limit <= 0 ? 20 : limit;
        return workflowRunRepository.findAll().stream()
                .map(this::toQueueItem)
                .flatMap(Optional::stream)
                .filter(predicate)
                .sorted(Comparator.comparing(WorkflowQueueItemView::updatedAt).reversed())
                .limit(normalizedLimit)
                .toList();
    }

    private Optional<WorkflowQueueItemView> toQueueItem(WorkflowRun run) {
        ReplyDraft draft = replyDraftRepository.findByRunId(run.getRunId()).orElse(null);
        if (draft == null) {
            return Optional.empty();
        }

        ReplyDispatch latestDispatch = replyDispatchRepository.findLatestByRunId(run.getRunId()).orElse(null);
        ReviewRecord latestReview = reviewRecordRepository.findByRunId(run.getRunId()).stream()
                .max(Comparator.comparing(ReviewRecord::getCreatedAt))
                .orElse(null);
        MailReceipt receipt = mailReceiptRepository.findByMessageId(run.getMessageId()).orElse(null);

        return Optional.of(new WorkflowQueueItemView(
                run.getRunId(),
                run.getMessageId(),
                run.getThreadId(),
                receipt == null ? null : receipt.getSender(),
                receipt == null ? draft.getSubject() : receipt.getSubject(),
                run.getStatus().name(),
                run.getStage().name(),
                draft.getStatus().name(),
                draft.getSendReadiness().name(),
                draft.getNextAction(),
                latestDispatch == null ? null : latestDispatch.getStatus().name(),
                latestDispatch == null ? null : latestDispatch.getAttemptCount(),
                latestDispatch == null ? null : latestDispatch.getNextRetryAt(),
                latestReview == null ? null : latestReview.getAction().name(),
                latestReview == null ? null : latestReview.getReviewer(),
                latestReview == null ? null : latestReview.getReviewNote(),
                run.getCreatedAt(),
                maxUpdatedAt(run, draft, latestDispatch, latestReview)
        ));
    }

    private java.time.OffsetDateTime maxUpdatedAt(WorkflowRun run,
                                                  ReplyDraft draft,
                                                  ReplyDispatch dispatch,
                                                  ReviewRecord review) {
        java.time.OffsetDateTime latest = run.getUpdatedAt();
        if (draft.getUpdatedAt().isAfter(latest)) {
            latest = draft.getUpdatedAt();
        }
        if (dispatch != null && dispatch.getUpdatedAt().isAfter(latest)) {
            latest = dispatch.getUpdatedAt();
        }
        if (review != null && review.getCreatedAt().isAfter(latest)) {
            latest = review.getCreatedAt();
        }
        return latest;
    }
}
