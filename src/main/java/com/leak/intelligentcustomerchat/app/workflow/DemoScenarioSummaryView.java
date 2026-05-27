package com.leak.intelligentcustomerchat.app.workflow;

public record DemoScenarioSummaryView(
        String scenarioId,
        String title,
        String scene,
        String subIntent,
        String description,
        String from,
        String subject,
        String recommendedMode,
        String demoFocus,
        String expectedResultType,
        String businessEvidenceHint,
        String knowledgeEvidenceHint
) {
}
