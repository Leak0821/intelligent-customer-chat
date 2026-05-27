package com.leak.intelligentcustomerchat.domain.review;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;

import java.util.List;
import java.util.Objects;

public record ReviewDecision(
        ReplyDraftStatus finalStatus,
        boolean autoSendAllowed,
        String reviewReason,
        List<String> reviewSignals
) {
    public ReviewDecision {
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");
        Objects.requireNonNull(reviewReason, "reviewReason must not be null");
        Objects.requireNonNull(reviewSignals, "reviewSignals must not be null");
        reviewSignals = List.copyOf(reviewSignals);
    }
}
