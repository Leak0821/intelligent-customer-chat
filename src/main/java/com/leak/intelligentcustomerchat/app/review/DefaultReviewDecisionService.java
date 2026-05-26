package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import org.springframework.stereotype.Service;

@Service
public class DefaultReviewDecisionService implements ReviewDecisionService {

    @Override
    public ReviewDecision review(ReplyDraft draft) {
        if (draft.getStatus() == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "high risk request stays in manual review");
        }
        if (draft.getStatus() == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return new ReviewDecision(ReplyDraftStatus.FOLLOW_UP_NEEDED, false, "missing key entity, follow-up required");
        }
        return new ReviewDecision(ReplyDraftStatus.DRAFT_READY, false, "draft ready for downstream review or send decision");
    }
}
