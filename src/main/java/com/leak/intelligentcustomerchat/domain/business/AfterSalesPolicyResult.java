package com.leak.intelligentcustomerchat.domain.business;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record AfterSalesPolicyResult(
        GatewayQueryStatus status,
        String sourceSystem,
        String policyCode,
        List<String> policyNotes,
        List<String> missingEntities,
        List<String> conflictFlags,
        OffsetDateTime queriedAt
) {
    public AfterSalesPolicyResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(sourceSystem, "sourceSystem must not be null");
        policyNotes = List.copyOf(policyNotes);
        missingEntities = List.copyOf(missingEntities);
        conflictFlags = List.copyOf(conflictFlags);
        Objects.requireNonNull(queriedAt, "queriedAt must not be null");
    }
}
