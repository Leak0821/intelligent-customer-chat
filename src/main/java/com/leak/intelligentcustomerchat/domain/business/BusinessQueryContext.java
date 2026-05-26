package com.leak.intelligentcustomerchat.domain.business;

import java.util.Objects;

public record BusinessQueryContext(
        String customerEmail,
        String threadId,
        String scenario,
        String intent,
        String orderId,
        String trackingNumber,
        String queryReason
) {
    public BusinessQueryContext {
        Objects.requireNonNull(customerEmail, "customerEmail must not be null");
        Objects.requireNonNull(threadId, "threadId must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(intent, "intent must not be null");
        Objects.requireNonNull(queryReason, "queryReason must not be null");
    }
}
