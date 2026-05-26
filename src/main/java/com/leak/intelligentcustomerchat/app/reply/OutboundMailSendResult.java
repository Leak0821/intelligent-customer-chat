package com.leak.intelligentcustomerchat.app.reply;

import java.time.OffsetDateTime;
import java.util.Objects;

public record OutboundMailSendResult(
        boolean success,
        String providerMessageId,
        String errorMessage,
        OffsetDateTime attemptedAt
) {
    public OutboundMailSendResult {
        Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    }

    public static OutboundMailSendResult success(String providerMessageId) {
        return new OutboundMailSendResult(true, providerMessageId, null, OffsetDateTime.now());
    }

    public static OutboundMailSendResult failed(String errorMessage) {
        return new OutboundMailSendResult(false, null, Objects.requireNonNull(errorMessage, "errorMessage must not be null"), OffsetDateTime.now());
    }
}
