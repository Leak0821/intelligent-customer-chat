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
        assertThat(executionView.summary().mode()).isEqualTo("replay");
        assertThat(executionView.summary().runId()).isNotBlank();
        assertThat(executionView.summary().scene()).isEqualTo("AFTER_SALES");
        assertThat(executionView.summary().riskLevel()).isNotBlank();
        assertThat(executionView.summary().releaseDecision()).isNotBlank();
        assertThat(executionView.summary().sendAllowed()).isFalse();
        assertThat(executionView.summary().businessFactStatus()).isEqualTo("CONFLICT");
        assertThat(executionView.summary().businessEvidence()).contains("conflict");
        assertThat(executionView.summary().knowledgeEvidence()).contains("knowledge");
        assertThat(executionView.summary().replyEvidence()).contains("replySource=");
        assertThat(executionView.summary().keyEvidence()).anyMatch(item -> item.startsWith("fact_source="));
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
        assertThat(executionView.summary().mode()).isEqualTo("review_loop");
        assertThat(executionView.summary().resultType()).isEqualTo("人工审核闭环");
        assertThat(executionView.summary().riskLevel()).isEqualTo("LOW");
        assertThat(executionView.summary().releaseDecision()).isEqualTo("READY_FOR_DISPATCH");
        assertThat(executionView.summary().sendAllowed()).isTrue();
        assertThat(executionView.summary().operatorDecision()).isEqualTo("manual_review_completed");
        assertThat(executionView.summary().nextAction()).isNotBlank();
        assertThat(executionView.summary().replyEvidence()).contains("latestReviewAction=APPROVE_SEND");
        assertThat(executionView.summary().keyEvidence()).contains("review_count=4");
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
        assertThat(reviewLoopView.evaluation().latestReviewAction()).isEqualTo("APPROVE_SEND");
        assertThat(reviewLoopView.evaluation().manualReviewOutcome()).isEqualTo("APPROVED_FOR_SEND");
        assertThat(reviewLoopView.evaluation().reviewActionCounts()).contains(
                new WorkflowEvaluationCountView("APPROVE_SEND", 1),
                new WorkflowEvaluationCountView("REJECT_SEND", 1),
                new WorkflowEvaluationCountView("REVISE_DRAFT", 1),
                new WorkflowEvaluationCountView("RESUBMIT_REVIEW", 1)
        );
        assertThat(reviewLoopView.evaluation().businessFactRole()).contains("order truth before policy guidance");
        assertThat(reviewLoopView.evaluation().knowledgeRole()).contains("policy wording");
        assertThat(reviewLoopView.evaluation().reviewTimeline()).anyMatch(item -> item.startsWith("APPROVE_SEND by demo-auditor-2"));
        assertThat(reviewLoopView.replay().latestDraft()).isNotNull();
    }

    @Test
    void shouldValidateAnalysisScenarioUsingRecommendedMode() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("after-sales-policy", "validate");

        assertThat(executionView.mode()).isEqualTo("validate");
        assertThat(executionView.summary().mode()).isEqualTo("validate");
        assertThat(executionView.summary().riskLevel()).isEqualTo("LOW");
        assertThat(executionView.summary().releaseDecision()).isEqualTo("EXPECTATION_CONFIRMED");
        assertThat(executionView.summary().sendAllowed()).isTrue();
        assertThat(executionView.summary().operatorDecision()).isEqualTo("scenario_expectation_matched");
        assertThat(executionView.summary().resultType()).isEqualTo("校验通过");
        assertThat(executionView.summary().nextAction()).isEqualTo("continue_demo");
        assertThat(executionView.summary().replyEvidence()).contains("validatedMode=analysis");
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
        assertThat(executionView.summary().mode()).isEqualTo("validate");
        assertThat(executionView.summary().replyEvidence()).contains("validatedMode=replay");
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
        assertThat(executionView.summary().mode()).isEqualTo("validate");
        assertThat(executionView.summary().replyEvidence()).contains("validatedMode=review_loop");
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

    @Test
    void shouldExecuteAnalysisModeWithTopLevelSummary() {
        DemoScenarioExecutionView executionView = demoScenarioCatalogService.execute("pre-sales-recommendation", "analysis");

        assertThat(executionView.mode()).isEqualTo("analysis");
        assertThat(executionView.summary().mode()).isEqualTo("analysis");
        assertThat(executionView.summary().runId()).isEqualTo("ANALYSIS_PREVIEW");
        assertThat(executionView.summary().scene()).isNotBlank();
        assertThat(executionView.summary().subIntent()).isNotBlank();
        assertThat(executionView.summary().riskLevel()).isNotBlank();
        assertThat(executionView.summary().releaseDecision()).isNotBlank();
        assertThat(executionView.summary().operatorDecision()).isNotBlank();
        assertThat(executionView.summary().nextAction()).isNotBlank();
        assertThat(executionView.summary().businessFactStatus()).isEqualTo("NOT_REQUIRED");
        assertThat(executionView.summary().businessEvidence()).contains("不依赖业务 facts");
        assertThat(executionView.summary().knowledgeEvidence()).contains("知识检索");
        assertThat(executionView.summary().replyEvidence()).contains("最终状态");
        assertThat(executionView.summary().keyEvidence()).anyMatch(item -> item.startsWith("knowledge_hint="));
        assertThat(executionView.result()).isInstanceOf(WorkflowAnalysisView.class);
    }
}
