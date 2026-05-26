package com.leak.intelligentcustomerchat.infrastructure.business;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record AfterSalesPolicyCatalogRecord(
        String policyCode,
        String policyName,
        List<String> applicableIntents,
        List<String> policyNotes,
        boolean enabled,
        OffsetDateTime updatedAt
) {
    public AfterSalesPolicyCatalogRecord {
        Objects.requireNonNull(policyCode, "policyCode must not be null");
        Objects.requireNonNull(policyName, "policyName must not be null");
        applicableIntents = List.copyOf(applicableIntents);
        policyNotes = List.copyOf(policyNotes);
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
