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
                        "after-sales-manual-review",
                        "after-sales-logistics",
                        "after-sales-policy",
                        "after-sales-missing-id",
                        "system-blocked-demo"
                );
        assertThat(scenarios.get(0).title()).isEqualTo("售前推荐样例");
        assertThat(scenarios.get(4).scene()).isEqualTo("AFTER_SALES");
        assertThat(scenarios.get(9).scene()).isEqualTo("SYSTEM");
        assertThat(scenarios.get(4).recommendedMode()).isEqualTo("replay");
        assertThat(scenarios.get(4).demoFocus()).contains("facts-first");
        assertThat(scenarios.get(4).expectedResultType()).isEqualTo("直接草稿");
        assertThat(scenarios.get(6).businessEvidenceHint()).contains("订单和物流 facts");
        assertThat(scenarios.get(7).knowledgeEvidenceHint()).contains("政策边界");
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
        assertThat(replayView.evidence().businessFactStatus()).isEqualTo("CONFLICT");
        assertThat(replayView.evidence().businessFactRole()).contains("authority check");
        assertThat(replayView.evidence().knowledgeRole()).contains("expectation setting");
        assertThat(replayView.evidence().knowledgeRecallCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldExecuteReviewLoopModeForManualReviewScenario() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("after-sales-manual-review", "review_loop");

        assertThat(executionView.mode()).isEqualTo("review_loop");
        assertThat(executionView.scenario().scenarioId()).isEqualTo("after-sales-manual-review");
        assertThat(executionView.result()).isInstanceOf(DemoReviewLoopExecutionView.class);

        DemoReviewLoopExecutionView reviewLoopView = (DemoReviewLoopExecutionView) executionView.result();
        assertThat(reviewLoopView.initialDraft().draftStatus()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        assertThat(reviewLoopView.rejectedDraft().sendReadiness()).isEqualTo("HOLD");
        assertThat(reviewLoopView.resubmittedDraft().draftVersion()).isEqualTo(2);
        assertThat(reviewLoopView.resubmittedDraft().nextAction()).isEqualTo("await_review_decision");
        assertThat(reviewLoopView.approvedDraft().sendReadiness()).isEqualTo("READY_FOR_SEND");
        assertThat(reviewLoopView.reviews()).hasSize(4);
        assertThat(reviewLoopView.evaluation().reviewCount()).isEqualTo(4);
        assertThat(reviewLoopView.evaluation().revisionCount()).isEqualTo(1);
        assertThat(reviewLoopView.evaluation().resubmittedForReview()).isTrue();
        assertThat(reviewLoopView.evaluation().businessFactRole()).contains("order truth before policy guidance");
        assertThat(reviewLoopView.evaluation().knowledgeRole()).contains("policy wording");
        assertThat(reviewLoopView.evaluation().reviewTimeline()).anyMatch(item -> item.startsWith("APPROVE_SEND by demo-auditor-2"));
        assertThat(reviewLoopView.replay().latestDraft()).isNotNull();
    }

    @Test
    void shouldValidateAnalysisScenarioUsingRecommendedMode() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("after-sales-policy", "validate");

        assertThat(executionView.mode()).isEqualTo("validate");
        assertThat(executionView.result()).isInstanceOf(DemoScenarioValidationView.class);

        DemoScenarioValidationView validationView = (DemoScenarioValidationView) executionView.result();
        assertThat(validationView.scenarioId()).isEqualTo("after-sales-policy");
        assertThat(validationView.validatedMode()).isEqualTo("analysis");
        assertThat(validationView.passed())
                .withFailMessage("validation checks=%s", validationView.checks())
                .isTrue();
        assertThat(validationView.checks())
                .extracting(DemoScenarioValidationCheckView::key)
                .containsExactly("scene", "workflowSubIntent", "draftStatus", "businessFactStatus", "resultType");
        assertThat(validationView.checks())
                .allMatch(DemoScenarioValidationCheckView::passed);
    }

    @Test
    void shouldValidateReplayScenarioUsingRecommendedMode() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("after-sales-order-status", "validate");

        assertThat(executionView.mode()).isEqualTo("validate");
        assertThat(executionView.result()).isInstanceOf(DemoScenarioValidationView.class);

        DemoScenarioValidationView validationView = (DemoScenarioValidationView) executionView.result();
        assertThat(validationView.scenarioId()).isEqualTo("after-sales-order-status");
        assertThat(validationView.validatedMode()).isEqualTo("replay");
        assertThat(validationView.passed())
                .withFailMessage("validation checks=%s", validationView.checks())
                .isTrue();
        assertThat(validationView.checks())
                .extracting(DemoScenarioValidationCheckView::key)
                .containsExactly("scene", "workflowSubIntent", "workflowStatus", "draftStatus", "businessFactStatus", "resultType");
        assertThat(validationView.checks())
                .allMatch(DemoScenarioValidationCheckView::passed);
    }

    @Test
    void shouldValidateReviewLoopScenarioUsingRecommendedMode() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("after-sales-manual-review", "validate");

        assertThat(executionView.mode()).isEqualTo("validate");
        assertThat(executionView.result()).isInstanceOf(DemoScenarioValidationView.class);

        DemoScenarioValidationView validationView = (DemoScenarioValidationView) executionView.result();
        assertThat(validationView.scenarioId()).isEqualTo("after-sales-manual-review");
        assertThat(validationView.validatedMode()).isEqualTo("review_loop");
        assertThat(validationView.passed())
                .withFailMessage("validation checks=%s", validationView.checks())
                .isTrue();
        assertThat(validationView.checks())
                .extracting(DemoScenarioValidationCheckView::key)
                .containsExactly("scene", "workflowSubIntent", "workflowStatus", "draftStatus", "businessFactStatus", "resultType");
        assertThat(validationView.checks())
                .allMatch(DemoScenarioValidationCheckView::passed);
    }
}
