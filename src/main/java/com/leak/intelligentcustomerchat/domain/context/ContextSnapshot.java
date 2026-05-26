package com.leak.intelligentcustomerchat.domain.context;

import java.util.List;
import java.util.Objects;

public record ContextSnapshot(
        String threadSummary,
        List<String> strongSignals,
        List<String> optionalSignals
) {
    public ContextSnapshot {
        Objects.requireNonNull(threadSummary, "threadSummary must not be null");
        strongSignals = List.copyOf(strongSignals);
        optionalSignals = List.copyOf(optionalSignals);
    }

    public static ContextSnapshot empty() {
        return new ContextSnapshot("", List.of(), List.of());
    }
}
