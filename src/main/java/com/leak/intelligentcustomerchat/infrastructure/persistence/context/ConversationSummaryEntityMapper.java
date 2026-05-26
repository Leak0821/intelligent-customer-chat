package com.leak.intelligentcustomerchat.infrastructure.persistence.context;

import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ConversationSummaryEntityMapper {
    private static final ZoneOffset STORAGE_ZONE_OFFSET = ZoneOffset.UTC;

    private ConversationSummaryEntityMapper() {
    }

    static ConversationSummaryEntity toEntity(ConversationSummary summary) {
        ConversationSummaryEntity entity = new ConversationSummaryEntity();
        entity.setSummaryId(summary.getSummaryId());
        entity.setThreadId(summary.getThreadId());
        entity.setSummaryText(summary.getSummaryText());
        entity.setSummarySource(summary.getSummarySource());
        entity.setCoveredMessageCount(summary.getCoveredMessageCount());
        entity.setCreatedAt(toLocalDateTime(summary.getCreatedAt()));
        return entity;
    }

    static ConversationSummary toDomain(ConversationSummaryEntity entity) {
        return ConversationSummary.restore(
                entity.getSummaryId(),
                entity.getThreadId(),
                entity.getSummaryText(),
                entity.getSummarySource(),
                entity.getCoveredMessageCount(),
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
