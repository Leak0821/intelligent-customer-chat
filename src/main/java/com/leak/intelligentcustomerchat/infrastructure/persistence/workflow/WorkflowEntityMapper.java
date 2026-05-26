package com.leak.intelligentcustomerchat.infrastructure.persistence.workflow;

import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class WorkflowEntityMapper {
    private static final ZoneOffset STORAGE_ZONE_OFFSET = ZoneOffset.UTC;

    private WorkflowEntityMapper() {
    }

    static WorkflowRunEntity toEntity(WorkflowRun run) {
        WorkflowRunEntity entity = new WorkflowRunEntity();
        entity.setRunId(run.getRunId());
        entity.setMessageId(run.getMessageId());
        entity.setThreadId(run.getThreadId());
        entity.setStage(run.getStage().name());
        entity.setStatus(run.getStatus().name());
        entity.setStatusReason(run.getStatusReason());
        entity.setCreatedAt(toLocalDateTime(run.getCreatedAt()));
        entity.setUpdatedAt(toLocalDateTime(run.getUpdatedAt()));
        return entity;
    }

    static WorkflowRun toDomain(WorkflowRunEntity entity) {
        return WorkflowRun.restore(
                entity.getRunId(),
                entity.getMessageId(),
                entity.getThreadId(),
                WorkflowStage.valueOf(entity.getStage()),
                WorkflowStatus.valueOf(entity.getStatus()),
                entity.getStatusReason(),
                toOffsetDateTime(entity.getCreatedAt()),
                toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    static WorkflowEventEntity toEntity(WorkflowEvent event) {
        WorkflowEventEntity entity = new WorkflowEventEntity();
        entity.setEventId(event.eventId());
        entity.setRunId(event.runId());
        entity.setMessageId(event.messageId());
        entity.setStage(event.stage().name());
        entity.setStatus(event.status().name());
        entity.setSummary(event.summary());
        entity.setCreatedAt(toLocalDateTime(event.createdAt()));
        return entity;
    }

    static WorkflowEvent toDomain(WorkflowEventEntity entity) {
        return WorkflowEvent.restore(
                entity.getEventId(),
                entity.getRunId(),
                entity.getMessageId(),
                WorkflowStage.valueOf(entity.getStage()),
                WorkflowStatus.valueOf(entity.getStatus()),
                entity.getSummary(),
                toOffsetDateTime(entity.getCreatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value.withOffsetSameInstant(STORAGE_ZONE_OFFSET).toLocalDateTime();
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value.atOffset(STORAGE_ZONE_OFFSET);
    }
}
