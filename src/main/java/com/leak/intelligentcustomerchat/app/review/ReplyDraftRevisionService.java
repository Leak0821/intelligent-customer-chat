package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class ReplyDraftRevisionService {
    private final ReplyDraftRepository replyDraftRepository;
    private final ReviewRecordRepository reviewRecordRepository;

    public ReplyDraftRevisionService(ReplyDraftRepository replyDraftRepository,
                                     ReviewRecordRepository reviewRecordRepository) {
        this.replyDraftRepository = replyDraftRepository;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    public ReplyDraft revise(String runId,
                             String editor,
                             String subject,
                             String body,
                             String revisionNote,
                             boolean submitForReview) {
        ReplyDraft draft = replyDraftRepository.findByRunId(runId)
                .orElseThrow(() -> new NoSuchElementException("draft not found for runId=" + runId));

        ReplyDraftStatus nextStatus = submitForReview ? ReplyDraftStatus.DRAFT_READY : ReplyDraftStatus.HUMAN_REVIEW_REQUIRED;
        String normalizedNote = revisionNote == null || revisionNote.isBlank()
                ? "draft revised manually"
                : revisionNote.trim();

        draft.revise(subject, body, nextStatus, normalizedNote, editor);
        if (submitForReview) {
            draft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "await_review_decision", normalizedNote);
        } else {
            draft.updateSendReadiness(SendReadiness.HOLD, "manual_review_required", normalizedNote);
        }
        replyDraftRepository.save(draft);
        reviewRecordRepository.save(ReviewRecord.reviseDraft(runId, draft.getDraftId(), editor, normalizedNote));
        if (submitForReview) {
            reviewRecordRepository.save(ReviewRecord.resubmitReview(runId, draft.getDraftId(), editor, normalizedNote));
        }
        return draft;
    }
}
