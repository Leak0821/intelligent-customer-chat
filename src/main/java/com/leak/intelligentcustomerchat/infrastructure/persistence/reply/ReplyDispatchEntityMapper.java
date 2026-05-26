package com.leak.intelligentcustomerchat.infrastructure.persistence.reply;

import com.leak.intelligentcustomerchat.domain.reply.DispatchTriggerSource;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ReplyDispatchEntityMapper {
    private static final ZoneOffset STORAGE_ZONE_OFFSET = ZoneOffset.UTC;

    private ReplyDispatchEntityMapper() {
    }

    static ReplyDispatchEntity toEntity(ReplyDispatch dispatch) {
        ReplyDispatchEntity entity = new ReplyDispatchEntity();
        entity.setDispatchId(dispatch.getDispatchId());
        entity.setRunId(dispatch.getRunId());
        entity.setDraftId(dispatch.getDraftId());
        entity.setRecipient(dispatch.getRecipient());
        entity.setSubject(dispatch.getSubject());
        entity.setBodySnapshot(dispatch.getBodySnapshot());
        entity.setAttemptCount(dispatch.getAttemptCount());
        entity.setMaxAttempts(dispatch.getMaxAttempts());
        entity.setStatus(dispatch.getStatus().name());
        entity.setLatestTriggerSource(dispatch.getLatestTriggerSource().name());
        entity.setLatestTriggeredBy(dispatch.getLatestTriggeredBy());
        entity.setLatestTriggerReason(dispatch.getLatestTriggerReason());
        entity.setProviderMessageId(dispatch.getProviderMessageId());
        entity.setErrorMessage(dispatch.getErrorMessage());
        entity.setLastAttemptAt(toLocalDateTime(dispatch.getLastAttemptAt()));
        entity.setNextRetryAt(toLocalDateTime(dispatch.getNextRetryAt()));
        entity.setCreatedAt(toLocalDateTime(dispatch.getCreatedAt()));
        entity.setUpdatedAt(toLocalDateTime(dispatch.getUpdatedAt()));
        return entity;
    }

    static ReplyDispatch toDomain(ReplyDispatchEntity entity) {
        return ReplyDispatch.restore(
                entity.getDispatchId(),
                entity.getRunId(),
                entity.getDraftId(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getBodySnapshot(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                ReplyDispatchStatus.valueOf(entity.getStatus()),
                DispatchTriggerSource.valueOf(entity.getLatestTriggerSource()),
                entity.getLatestTriggeredBy(),
                entity.getLatestTriggerReason(),
                entity.getProviderMessageId(),
                entity.getErrorMessage(),
                toOffsetDateTime(entity.getLastAttemptAt()),
                toOffsetDateTime(entity.getNextRetryAt()),
                toOffsetDateTime(entity.getCreatedAt()),
                toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.withOffsetSameInstant(STORAGE_ZONE_OFFSET).toLocalDateTime();
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(STORAGE_ZONE_OFFSET);
    }
}
