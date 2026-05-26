package com.leak.intelligentcustomerchat.domain.mail;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class MailReceipt {
    private final String receiptId;
    private final String sourceKey;
    private final String folderName;
    private final long uid;
    private final String messageId;
    private final String threadId;
    private final String sender;
    private final String subject;
    private final OffsetDateTime receivedAt;
    private MailReceiptStatus status;
    private String workflowRunId;
    private String errorMessage;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private MailReceipt(String receiptId,
                        String sourceKey,
                        String folderName,
                        long uid,
                        String messageId,
                        String threadId,
                        String sender,
                        String subject,
                        OffsetDateTime receivedAt,
                        MailReceiptStatus status,
                        String workflowRunId,
                        String errorMessage,
                        OffsetDateTime createdAt,
                        OffsetDateTime updatedAt) {
        this.receiptId = Objects.requireNonNull(receiptId, "receiptId must not be null");
        this.sourceKey = Objects.requireNonNull(sourceKey, "sourceKey must not be null");
        this.folderName = Objects.requireNonNull(folderName, "folderName must not be null");
        this.uid = uid;
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.threadId = Objects.requireNonNull(threadId, "threadId must not be null");
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.workflowRunId = workflowRunId;
        this.errorMessage = errorMessage;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static MailReceipt fetched(String receiptId,
                                      String sourceKey,
                                      String folderName,
                                      long uid,
                                      InboundMail mail) {
        OffsetDateTime now = OffsetDateTime.now();
        return new MailReceipt(
                receiptId,
                sourceKey,
                folderName,
                uid,
                mail.messageId(),
                mail.threadId(),
                mail.from(),
                mail.subject(),
                mail.receivedAt(),
                MailReceiptStatus.FETCHED,
                null,
                null,
                now,
                now
        );
    }

    public static MailReceipt restore(String receiptId,
                                      String sourceKey,
                                      String folderName,
                                      long uid,
                                      String messageId,
                                      String threadId,
                                      String sender,
                                      String subject,
                                      OffsetDateTime receivedAt,
                                      MailReceiptStatus status,
                                      String workflowRunId,
                                      String errorMessage,
                                      OffsetDateTime createdAt,
                                      OffsetDateTime updatedAt) {
        return new MailReceipt(receiptId, sourceKey, folderName, uid, messageId, threadId, sender, subject,
                receivedAt, status, workflowRunId, errorMessage, createdAt, updatedAt);
    }

    public void markProcessed(String runId) {
        this.status = MailReceiptStatus.PROCESSED;
        this.workflowRunId = Objects.requireNonNull(runId, "runId must not be null");
        this.errorMessage = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = MailReceiptStatus.FAILED;
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        this.updatedAt = OffsetDateTime.now();
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getFolderName() {
        return folderName;
    }

    public long getUid() {
        return uid;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public MailReceiptStatus getStatus() {
        return status;
    }

    public String getWorkflowRunId() {
        return workflowRunId;
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
