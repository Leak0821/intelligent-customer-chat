package com.leak.intelligentcustomerchat.app.workflow;

import java.util.List;
import java.util.Objects;

public record DemoScenarioValidationView(
        String scenarioId,
        String validatedMode,
        boolean passed,
        List<DemoScenarioValidationCheckView> checks
) {
    public DemoScenarioValidationView {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(validatedMode, "validatedMode must not be null");
        Objects.requireNonNull(checks, "checks must not be null");
        checks = List.copyOf(checks);
    }
}
