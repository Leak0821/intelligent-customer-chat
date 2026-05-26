package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultReviewDecisionService implements ReviewDecisionService {

    @Override
    public ReviewDecision review(ReplyDraft draft) {
        Objects.requireNonNull(draft, "draft must not be null");
        if (draft.isHumanReviewRequired()) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "high risk request stays in manual review");
        }
        if (draft.isFollowUpNeeded()) {
            return new ReviewDecision(ReplyDraftStatus.FOLLOW_UP_NEEDED, false, "missing key entity, follow-up required");
        }
        return new ReviewDecision(ReplyDraftStatus.DRAFT_READY, false, "draft ready for downstream review or send decision");
    }
}
