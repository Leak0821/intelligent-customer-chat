package com.leak.intelligentcustomerchat.app.workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record WorkflowBusinessFactDiagnosticsView(
        String factStatus,
        List<String> sourceSystems,
        List<String> requiredFactTypes,
        String factRole,
        List<String> resolvedEntities,
        List<String> facts,
        List<String> missingEntities,
        List<String> conflictFlags,
        OffsetDateTime factTimestamp
) {
    public WorkflowBusinessFactDiagnosticsView {
        Objects.requireNonNull(factStatus, "factStatus must not be null");
        Objects.requireNonNull(sourceSystems, "sourceSystems must not be null");
        Objects.requireNonNull(requiredFactTypes, "requiredFactTypes must not be null");
        Objects.requireNonNull(factRole, "factRole must not be null");
        Objects.requireNonNull(resolvedEntities, "resolvedEntities must not be null");
        Objects.requireNonNull(facts, "facts must not be null");
        Objects.requireNonNull(missingEntities, "missingEntities must not be null");
        Objects.requireNonNull(conflictFlags, "conflictFlags must not be null");
        Objects.requireNonNull(factTimestamp, "factTimestamp must not be null");
        sourceSystems = List.copyOf(sourceSystems);
        requiredFactTypes = List.copyOf(requiredFactTypes);
        resolvedEntities = List.copyOf(resolvedEntities);
        facts = List.copyOf(facts);
        missingEntities = List.copyOf(missingEntities);
        conflictFlags = List.copyOf(conflictFlags);
    }
}
