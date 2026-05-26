package com.leak.intelligentcustomerchat.domain.business;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record BusinessFactResult(
        BusinessFactStatus status,
        String sourceSystem,
        List<String> resolvedEntities,
        List<String> facts,
        List<String> missingEntities,
        List<String> conflictFlags,
        OffsetDateTime factTimestamp
) {
    public BusinessFactResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(sourceSystem, "sourceSystem must not be null");
        resolvedEntities = List.copyOf(resolvedEntities);
        facts = List.copyOf(facts);
        missingEntities = List.copyOf(missingEntities);
        conflictFlags = List.copyOf(conflictFlags);
        Objects.requireNonNull(factTimestamp, "factTimestamp must not be null");
    }

    public static BusinessFactResult notRequired() {
        return new BusinessFactResult(BusinessFactStatus.NOT_REQUIRED, "none", List.of(), List.of(), List.of(), List.of(), OffsetDateTime.now());
    }

    public static BusinessFactResult insufficientInput(List<String> missingEntities) {
        return new BusinessFactResult(BusinessFactStatus.INSUFFICIENT_INPUT, "business-gateway", List.of(), List.of(), missingEntities, List.of(), OffsetDateTime.now());
    }
}
