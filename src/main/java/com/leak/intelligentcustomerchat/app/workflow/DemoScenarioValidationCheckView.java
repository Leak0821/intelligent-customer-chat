package com.leak.intelligentcustomerchat.app.workflow;

import java.util.Objects;

public record DemoScenarioValidationCheckView(
        String key,
        String expected,
        String actual,
        boolean passed
) {
    public DemoScenarioValidationCheckView {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(expected, "expected must not be null");
        Objects.requireNonNull(actual, "actual must not be null");
    }
}
