package com.leak.intelligentcustomerchat.infrastructure.persistence.reply;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ReplyDraftEntityMapper {
    private static final ZoneOffset STORAGE_ZONE_OFFSET = ZoneOffset.UTC;

    private ReplyDraftEntityMapper() {
    }

    static ReplyDraftEntity toEntity(ReplyDraft draft) {
        ReplyDraftEntity entity = new ReplyDraftEntity();
        entity.setDraftId(draft.getDraftId());
        entity.setRunId(draft.getRunId());
        entity.setSubject(draft.getSubject());
        entity.setBody(draft.getBody());
        entity.setStatus(draft.getStatus().name());
        entity.setReviewNotes(draft.getReviewNotes());
        entity.setCreatedAt(toLocalDateTime(draft.getCreatedAt()));
        entity.setUpdatedAt(toLocalDateTime(draft.getUpdatedAt()));
        return entity;
    }

    static ReplyDraft toDomain(ReplyDraftEntity entity) {
        return ReplyDraft.restore(
                entity.getDraftId(),
                entity.getRunId(),
                entity.getSubject(),
                entity.getBody(),
                ReplyDraftStatus.valueOf(entity.getStatus()),
                entity.getReviewNotes(),
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
