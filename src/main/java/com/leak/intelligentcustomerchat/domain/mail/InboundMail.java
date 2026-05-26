package com.leak.intelligentcustomerchat.domain.mail;

import java.time.OffsetDateTime;
import java.util.Objects;

public record InboundMail(
        String messageId,
        String threadId,
        String from,
        String subject,
        String rawBody,
        OffsetDateTime receivedAt
) {
    public InboundMail {
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(threadId, "threadId must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(rawBody, "rawBody must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    }

    public InboundMail withRawBody(String cleanedBody) {
        return new InboundMail(messageId, threadId, from, subject, cleanedBody, receivedAt);
    }
}
