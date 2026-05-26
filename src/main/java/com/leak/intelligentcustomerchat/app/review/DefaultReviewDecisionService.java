package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultReviewDecisionService implements ReviewDecisionService {

    @Override
    public ReviewDecision review(ReplyDraft draft, ReviewDecisionContext context) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(context, "context must not be null");
        if (draft.isBlocked()) {
            return new ReviewDecision(ReplyDraftStatus.BLOCKED, false, "draft is blocked and requires manual investigation");
        }
        if (draft.isHumanReviewRequired()) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "high risk request stays in manual review");
        }
        if (draft.isFollowUpNeeded()) {
            return new ReviewDecision(ReplyDraftStatus.FOLLOW_UP_NEEDED, false, "missing key entity, follow-up required");
        }
        if (context.routeResult().scene() == CustomerScene.UNKNOWN) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "unknown scene requires manual review");
        }
        if (context.businessFactResult().status() == BusinessFactStatus.CONFLICT) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "business facts conflict, manual review required");
        }
        if (context.businessFactResult().status() == BusinessFactStatus.TEMPORARY_FAILURE) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "business facts unavailable, manual review required");
        }
        if (context.businessFactResult().status() == BusinessFactStatus.NO_RESULT
                && context.routeResult().scene() == CustomerScene.AFTER_SALES) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "after-sales facts not found, manual review required");
        }
        if (context.routeResult().scene() == CustomerScene.PRE_SALES
                && context.knowledgeRetrieveResult().recallCount() == 0) {
            return new ReviewDecision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "pre-sales answer lacks knowledge support");
        }
        return new ReviewDecision(ReplyDraftStatus.DRAFT_READY, false, "draft ready for downstream review or send decision");
    }
}
