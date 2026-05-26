package com.leak.intelligentcustomerchat.infrastructure.business;

import java.time.OffsetDateTime;
import java.util.Objects;

public record OrderCatalogRecord(
        String orderId,
        String customerEmail,
        String orderStatus,
        String paymentStatus,
        String trackingNumber,
        OffsetDateTime orderCreatedAt
) {
    public OrderCatalogRecord {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(customerEmail, "customerEmail must not be null");
        Objects.requireNonNull(orderStatus, "orderStatus must not be null");
        Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
        Objects.requireNonNull(trackingNumber, "trackingNumber must not be null");
        Objects.requireNonNull(orderCreatedAt, "orderCreatedAt must not be null");
    }
}
