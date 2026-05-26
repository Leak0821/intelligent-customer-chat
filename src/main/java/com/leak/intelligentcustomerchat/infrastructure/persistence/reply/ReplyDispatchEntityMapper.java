package com.leak.intelligentcustomerchat.infrastructure.persistence.reply;

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
        entity.setStatus(dispatch.getStatus().name());
        entity.setProviderMessageId(dispatch.getProviderMessageId());
        entity.setErrorMessage(dispatch.getErrorMessage());
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
                ReplyDispatchStatus.valueOf(entity.getStatus()),
                entity.getProviderMessageId(),
                entity.getErrorMessage(),
                toOffsetDateTime(entity.getCreatedAt()),
                toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value.withOffsetSameInstant(STORAGE_ZONE_OFFSET).toLocalDateTime();
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value.atOffset(STORAGE_ZONE_OFFSET);
    }
}
