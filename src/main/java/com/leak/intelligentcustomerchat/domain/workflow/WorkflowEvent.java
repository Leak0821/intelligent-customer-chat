package com.leak.intelligentcustomerchat.domain.workflow;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record WorkflowEvent(
        String eventId,
        String runId,
        String messageId,
        WorkflowStage stage,
        WorkflowStatus status,
        String summary,
        OffsetDateTime createdAt
) {
    public WorkflowEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(stage, "stage must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static WorkflowEvent fromRun(WorkflowRun run, String summary) {
        return new WorkflowEvent(
                UUID.randomUUID().toString(),
                run.getRunId(),
                run.getMessageId(),
                run.getStage(),
                run.getStatus(),
                summary,
                OffsetDateTime.now()
        );
    }
}
