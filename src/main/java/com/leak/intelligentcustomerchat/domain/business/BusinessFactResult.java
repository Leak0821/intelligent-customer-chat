package com.leak.intelligentcustomerchat.domain.business;

import java.util.List;
import java.util.Objects;

public record BusinessFactResult(
        BusinessFactStatus status,
        List<String> facts,
        List<String> missingEntities
) {
    public BusinessFactResult {
        Objects.requireNonNull(status, "status must not be null");
        facts = List.copyOf(facts);
        missingEntities = List.copyOf(missingEntities);
    }
}
