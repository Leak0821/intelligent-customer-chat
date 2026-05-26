package com.leak.intelligentcustomerchat.domain.reply;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class ReplyDispatch {
    private final String dispatchId;
    private final String runId;
    private final String draftId;
    private final String recipient;
    private final String subject;
    private final String bodySnapshot;
    private ReplyDispatchStatus status;
    private String providerMessageId;
    private String errorMessage;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private ReplyDispatch(String dispatchId,
                          String runId,
                          String draftId,
                          String recipient,
                          String subject,
                          String bodySnapshot,
                          ReplyDispatchStatus status,
                          String providerMessageId,
                          String errorMessage,
                          OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {
        this.dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.bodySnapshot = Objects.requireNonNull(bodySnapshot, "bodySnapshot must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ReplyDispatch sent(String runId,
                                     String draftId,
                                     String recipient,
                                     String subject,
                                     String bodySnapshot,
                                     String providerMessageId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReplyDispatch(
                UUID.randomUUID().toString(),
                runId,
                draftId,
                recipient,
                subject,
                bodySnapshot,
                ReplyDispatchStatus.SENT,
                providerMessageId,
                null,
                now,
                now
        );
    }

    public static ReplyDispatch failed(String runId,
                                       String draftId,
                                       String recipient,
                                       String subject,
                                       String bodySnapshot,
                                       String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReplyDispatch(
                UUID.randomUUID().toString(),
                runId,
                draftId,
                recipient,
                subject,
                bodySnapshot,
                ReplyDispatchStatus.FAILED,
                null,
                Objects.requireNonNull(errorMessage, "errorMessage must not be null"),
                now,
                now
        );
    }

    public static ReplyDispatch restore(String dispatchId,
                                        String runId,
                                        String draftId,
                                        String recipient,
                                        String subject,
                                        String bodySnapshot,
                                        ReplyDispatchStatus status,
                                        String providerMessageId,
                                        String errorMessage,
                                        OffsetDateTime createdAt,
                                        OffsetDateTime updatedAt) {
        return new ReplyDispatch(dispatchId, runId, draftId, recipient, subject, bodySnapshot, status, providerMessageId, errorMessage, createdAt, updatedAt);
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public String getRunId() {
        return runId;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodySnapshot() {
        return bodySnapshot;
    }

    public ReplyDispatchStatus getStatus() {
        return status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
