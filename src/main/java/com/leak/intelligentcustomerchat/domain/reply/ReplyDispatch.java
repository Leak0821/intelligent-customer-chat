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
    private int attemptCount;
    private int maxAttempts;
    private ReplyDispatchStatus status;
    private String providerMessageId;
    private String errorMessage;
    private OffsetDateTime lastAttemptAt;
    private OffsetDateTime nextRetryAt;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private ReplyDispatch(String dispatchId,
                          String runId,
                          String draftId,
                          String recipient,
                          String subject,
                          String bodySnapshot,
                          int attemptCount,
                          int maxAttempts,
                          ReplyDispatchStatus status,
                          String providerMessageId,
                          String errorMessage,
                          OffsetDateTime lastAttemptAt,
                          OffsetDateTime nextRetryAt,
                          OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {
        this.dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.bodySnapshot = Objects.requireNonNull(bodySnapshot, "bodySnapshot must not be null");
        this.attemptCount = validateAttemptCount(attemptCount);
        this.maxAttempts = validateMaxAttempts(maxAttempts);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ReplyDispatch create(String runId,
                                       String draftId,
                                       String recipient,
                                       String subject,
                                       String bodySnapshot,
                                       int maxAttempts) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReplyDispatch(
                UUID.randomUUID().toString(),
                runId,
                draftId,
                recipient,
                subject,
                bodySnapshot,
                0,
                maxAttempts,
                ReplyDispatchStatus.PENDING,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public void markAttemptResult(boolean success,
                                  String providerMessageId,
                                  String errorMessage,
                                  OffsetDateTime attemptedAt,
                                  OffsetDateTime nextRetryAt) {
        this.attemptCount += 1;
        this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
        this.updatedAt = attemptedAt;
        if (success) {
            this.status = ReplyDispatchStatus.SENT;
            this.providerMessageId = providerMessageId;
            this.errorMessage = null;
            this.nextRetryAt = null;
            return;
        }
        this.providerMessageId = null;
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        if (nextRetryAt == null) {
            this.status = ReplyDispatchStatus.FAILED_FINAL;
            this.nextRetryAt = null;
            return;
        }
        this.status = ReplyDispatchStatus.RETRY_PENDING;
        this.nextRetryAt = nextRetryAt;
    }

    public static ReplyDispatch restore(String dispatchId,
                                        String runId,
                                        String draftId,
                                        String recipient,
                                        String subject,
                                        String bodySnapshot,
                                        int attemptCount,
                                        int maxAttempts,
                                        ReplyDispatchStatus status,
                                        String providerMessageId,
                                        String errorMessage,
                                        OffsetDateTime lastAttemptAt,
                                        OffsetDateTime nextRetryAt,
                                        OffsetDateTime createdAt,
                                        OffsetDateTime updatedAt) {
        return new ReplyDispatch(
                dispatchId,
                runId,
                draftId,
                recipient,
                subject,
                bodySnapshot,
                attemptCount,
                maxAttempts,
                status,
                providerMessageId,
                errorMessage,
                lastAttemptAt,
                nextRetryAt,
                createdAt,
                updatedAt
        );
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

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
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

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isRetryPending() {
        return status == ReplyDispatchStatus.RETRY_PENDING;
    }

    public boolean isSent() {
        return status == ReplyDispatchStatus.SENT;
    }

    public boolean isFailedFinal() {
        return status == ReplyDispatchStatus.FAILED_FINAL;
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts;
    }

    private static int validateAttemptCount(int attemptCount) {
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must be >= 0");
        }
        return attemptCount;
    }

    private static int validateMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        return maxAttempts;
    }
}
