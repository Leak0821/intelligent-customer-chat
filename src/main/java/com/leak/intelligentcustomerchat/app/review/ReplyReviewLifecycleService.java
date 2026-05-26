package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.app.reply.ReplySendLifecycleService;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReplyReviewLifecycleService {
    private final ReplyDraftRepository replyDraftRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final ReplySendLifecycleService replySendLifecycleService;

    public ReplyReviewLifecycleService(ReplyDraftRepository replyDraftRepository,
                                      ReviewRecordRepository reviewRecordRepository,
                                      ReplySendLifecycleService replySendLifecycleService) {
        this.replyDraftRepository = replyDraftRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.replySendLifecycleService = replySendLifecycleService;
    }

    public ReplyDraft approveForSend(String runId, String reviewer, String reviewNote) {
        ReplyDraft draft = replySendLifecycleService.approveForSend(runId, reviewNote);
        reviewRecordRepository.save(ReviewRecord.approveSend(runId, draft.getDraftId(), reviewer, reviewNote));
        return draft;
    }

    public ReplyDraft rejectSend(String runId, String reviewer, String reviewNote) {
        ReplyDraft draft = requireDraft(runId);
        draft.revise(draft.getSubject(), draft.getBody(), ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, reviewNote);
        draft.updateSendReadiness(SendReadiness.HOLD, "manual_review_required", reviewNote);
        replyDraftRepository.save(draft);
        reviewRecordRepository.save(ReviewRecord.rejectSend(runId, draft.getDraftId(), reviewer, reviewNote));
        return draft;
    }

    public List<ReviewRecord> findReviews(String runId) {
        return reviewRecordRepository.findByRunId(runId);
    }

    private ReplyDraft requireDraft(String runId) {
        return replyDraftRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("draft not found for runId=" + runId));
    }
}
