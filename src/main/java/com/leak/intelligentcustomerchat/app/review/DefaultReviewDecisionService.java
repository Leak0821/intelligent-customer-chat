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
    private final ReviewFeedbackTagger reviewFeedbackTagger;

    public DefaultReviewDecisionService(ReviewFeedbackTagger reviewFeedbackTagger) {
        this.reviewFeedbackTagger = reviewFeedbackTagger;
    }

    @Override
    public ReviewDecision review(ReplyDraft draft, ReviewDecisionContext context) {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(context, "context must not be null");
        if (draft.isBlocked()) {
            return decision(ReplyDraftStatus.BLOCKED, false, "draft is blocked and requires manual investigation", draft, context);
        }
        if (draft.isHumanReviewRequired()) {
            return decision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "high risk request stays in manual review", draft, context);
        }
        if (draft.isFollowUpNeeded()) {
            return decision(ReplyDraftStatus.FOLLOW_UP_NEEDED, false, "missing key entity, follow-up required", draft, context);
        }
        if (context.routeResult().scene() == CustomerScene.UNKNOWN) {
            return decision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "unknown scene requires manual review", draft, context);
        }
        if (context.businessFactResult().status() == BusinessFactStatus.CONFLICT) {
            return decision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "business facts conflict, manual review required", draft, context);
        }
        if (context.businessFactResult().status() == BusinessFactStatus.TEMPORARY_FAILURE) {
            return decision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "business facts unavailable, manual review required", draft, context);
        }
        if (context.businessFactResult().status() == BusinessFactStatus.NO_RESULT
                && context.routeResult().scene() == CustomerScene.AFTER_SALES) {
            return decision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "after-sales facts not found, manual review required", draft, context);
        }
        if (context.routeResult().scene() == CustomerScene.PRE_SALES
                && context.knowledgeRetrieveResult().recallCount() == 0) {
            return decision(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, false, "pre-sales answer lacks knowledge support", draft, context);
        }
        return decision(ReplyDraftStatus.DRAFT_READY, false, "draft ready for downstream review or send decision", draft, context);
    }

    private ReviewDecision decision(ReplyDraftStatus finalStatus,
                                    boolean autoSendAllowed,
                                    String reviewReason,
                                    ReplyDraft draft,
                                    ReviewDecisionContext context) {
        return new ReviewDecision(
                finalStatus,
                autoSendAllowed,
                reviewReason,
                reviewFeedbackTagger.tagDecision(draft, context, reviewReason)
        );
    }
}
