package com.leak.intelligentcustomerchat.domain.workflow;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class WorkflowRun {
    private final String runId;
    private final String messageId;
    private final String threadId;
    private WorkflowStage stage;
    private WorkflowStatus status;
    private String statusReason;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private WorkflowRun(String runId, String messageId, String threadId, WorkflowStage stage,
                        WorkflowStatus status, String statusReason, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.threadId = Objects.requireNonNull(threadId, "threadId must not be null");
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.statusReason = Objects.requireNonNull(statusReason, "statusReason must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static WorkflowRun start(String messageId, String threadId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new WorkflowRun(
                UUID.randomUUID().toString(),
                messageId,
                threadId,
                WorkflowStage.MAIL_RECEIVED,
                WorkflowStatus.RUNNING,
                "mail received",
                now,
                now
        );
    }

    public static WorkflowRun restore(String runId,
                                      String messageId,
                                      String threadId,
                                      WorkflowStage stage,
                                      WorkflowStatus status,
                                      String statusReason,
                                      OffsetDateTime createdAt,
                                      OffsetDateTime updatedAt) {
        return new WorkflowRun(runId, messageId, threadId, stage, status, statusReason, createdAt, updatedAt);
    }

    public void moveTo(WorkflowStage nextStage, String reason) {
        this.stage = Objects.requireNonNull(nextStage, "nextStage must not be null");
        this.statusReason = Objects.requireNonNull(reason, "reason must not be null");
        this.updatedAt = OffsetDateTime.now();
    }

    public void complete(String reason) {
        this.stage = WorkflowStage.COMPLETED;
        this.status = WorkflowStatus.COMPLETED;
        this.statusReason = Objects.requireNonNull(reason, "reason must not be null");
        this.updatedAt = OffsetDateTime.now();
    }

    public void block(String reason) {
        this.stage = WorkflowStage.BLOCKED;
        this.status = WorkflowStatus.BLOCKED;
        this.statusReason = Objects.requireNonNull(reason, "reason must not be null");
        this.updatedAt = OffsetDateTime.now();
    }

    public String getRunId() {
        return runId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public WorkflowStage getStage() {
        return stage;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
