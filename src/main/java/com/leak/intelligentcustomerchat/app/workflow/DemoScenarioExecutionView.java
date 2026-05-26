package com.leak.intelligentcustomerchat.app.workflow;

public record DemoScenarioExecutionView(
        DemoScenarioSummaryView scenario,
        String mode,
        Object result
) {
}
