package com.leak.intelligentcustomerchat.app.reply;

import java.util.Objects;

public record OutboundMailRequest(
        String recipient,
        String subject,
        String body
) {
    public OutboundMailRequest {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
