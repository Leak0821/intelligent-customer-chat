package com.leak.intelligentcustomerchat.app.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DemoScenarioCatalogServiceTest {
    @Autowired
    private DemoScenarioCatalogService demoScenarioCatalogService;

    @Test
    void shouldExposeBuiltInScenarioCatalogInExpectedOrder() {
        var scenarios = demoScenarioCatalogService.listScenarios();

        assertThat(scenarios)
                .extracting(DemoScenarioSummaryView::scenarioId)
                .containsExactly(
                        "pre-sales-recommendation",
                        "pre-sales-comparison",
                        "pre-sales-general-inquiry",
                        "pre-sales-shipping-stock",
                        "after-sales-order-status",
                        "after-sales-logistics",
                        "after-sales-policy",
                        "after-sales-missing-id"
                );
        assertThat(scenarios.get(0).title()).isEqualTo("售前推荐样例");
        assertThat(scenarios.get(4).scene()).isEqualTo("AFTER_SALES");
    }

    @Test
    void shouldExecuteReplayModeForBuiltInScenario() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("after-sales-logistics", "replay");

        assertThat(executionView.mode()).isEqualTo("replay");
        assertThat(executionView.scenario().scenarioId()).isEqualTo("after-sales-logistics");
        assertThat(executionView.result()).isInstanceOf(WorkflowReplayView.class);

        WorkflowReplayView replayView = (WorkflowReplayView) executionView.result();
        assertThat(replayView.run().getRunId()).isNotBlank();
        assertThat(replayView.latestDraft()).isNotNull();
        assertThat(replayView.events()).isNotEmpty();
    }
}
