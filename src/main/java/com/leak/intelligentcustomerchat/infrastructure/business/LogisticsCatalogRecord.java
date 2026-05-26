package com.leak.intelligentcustomerchat.infrastructure.business;

import java.time.OffsetDateTime;
import java.util.Objects;

public record LogisticsCatalogRecord(
        String trackingNumber,
        String orderId,
        String logisticsStatus,
        String latestNode,
        String etaHint,
        OffsetDateTime updatedAt
) {
    public LogisticsCatalogRecord {
        Objects.requireNonNull(trackingNumber, "trackingNumber must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(logisticsStatus, "logisticsStatus must not be null");
        Objects.requireNonNull(latestNode, "latestNode must not be null");
        Objects.requireNonNull(etaHint, "etaHint must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
