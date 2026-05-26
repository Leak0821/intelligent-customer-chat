package com.leak.intelligentcustomerchat.domain.review;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;

import java.util.Objects;

public record ReviewDecision(
        ReplyDraftStatus finalStatus,
        boolean autoSendAllowed,
        String reviewReason
) {
    public ReviewDecision {
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");
        Objects.requireNonNull(reviewReason, "reviewReason must not be null");
    }
}
