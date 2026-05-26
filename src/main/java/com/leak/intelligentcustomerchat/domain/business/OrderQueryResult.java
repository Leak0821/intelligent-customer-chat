package com.leak.intelligentcustomerchat.domain.business;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record OrderQueryResult(
        GatewayQueryStatus status,
        String sourceSystem,
        String orderId,
        String orderStatus,
        String paymentStatus,
        String customerEmail,
        List<String> missingEntities,
        List<String> conflictFlags,
        OffsetDateTime queriedAt
) {
    public OrderQueryResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(sourceSystem, "sourceSystem must not be null");
        missingEntities = List.copyOf(missingEntities);
        conflictFlags = List.copyOf(conflictFlags);
        Objects.requireNonNull(queriedAt, "queriedAt must not be null");
    }

    public static OrderQueryResult insufficientInput(String sourceSystem, List<String> missingEntities) {
        return new OrderQueryResult(
                GatewayQueryStatus.INSUFFICIENT_INPUT,
                sourceSystem,
                null,
                null,
                null,
                null,
                missingEntities,
                List.of(),
                OffsetDateTime.now()
        );
    }
}
