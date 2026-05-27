package com.leak.intelligentcustomerchat.app.context;

import java.util.List;
import java.util.Objects;

public record ContextEntitySignals(
        String orderId,
        String trackingNumber,
        List<String> strongSignals
) {
    public ContextEntitySignals {
        Objects.requireNonNull(strongSignals, "strongSignals must not be null");
        strongSignals = List.copyOf(strongSignals);
    }

    public boolean hasReusableIdentifier() {
        return (orderId != null && !orderId.isBlank()) || (trackingNumber != null && !trackingNumber.isBlank());
    }
}
