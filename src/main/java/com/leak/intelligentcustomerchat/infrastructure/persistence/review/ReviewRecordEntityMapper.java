package com.leak.intelligentcustomerchat.infrastructure.persistence.review;

import com.leak.intelligentcustomerchat.domain.review.ReviewAction;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ReviewRecordEntityMapper {
    private static final ZoneOffset STORAGE_ZONE_OFFSET = ZoneOffset.UTC;

    private ReviewRecordEntityMapper() {
    }

    static ReviewRecordEntity toEntity(ReviewRecord reviewRecord) {
        ReviewRecordEntity entity = new ReviewRecordEntity();
        entity.setReviewId(reviewRecord.getReviewId());
        entity.setRunId(reviewRecord.getRunId());
        entity.setDraftId(reviewRecord.getDraftId());
        entity.setAction(reviewRecord.getAction().name());
        entity.setReviewer(reviewRecord.getReviewer());
        entity.setReviewNote(reviewRecord.getReviewNote());
        entity.setCreatedAt(toLocalDateTime(reviewRecord.getCreatedAt()));
        return entity;
    }

    static ReviewRecord toDomain(ReviewRecordEntity entity) {
        return ReviewRecord.restore(
                entity.getReviewId(),
                entity.getRunId(),
                entity.getDraftId(),
                ReviewAction.valueOf(entity.getAction()),
                entity.getReviewer(),
                entity.getReviewNote(),
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
