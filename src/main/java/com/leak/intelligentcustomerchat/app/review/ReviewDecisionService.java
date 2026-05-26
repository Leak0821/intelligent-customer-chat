package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;

public interface ReviewDecisionService {
    ReviewDecision review(ReplyDraft draft);
}
