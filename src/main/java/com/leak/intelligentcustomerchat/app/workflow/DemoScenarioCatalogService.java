package com.leak.intelligentcustomerchat.app.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class DemoScenarioCatalogService {
    private final MailIngestionService mailIngestionService;
    private final WorkflowRunService workflowRunService;
    private final WorkflowAnalysisService workflowAnalysisService;
    private final WorkflowEvaluationService workflowEvaluationService;
    private final DemoReviewLoopService demoReviewLoopService;
    private final Map<String, DemoScenarioDefinition> scenarioMap;

    public DemoScenarioCatalogService(ObjectMapper objectMapper,
                                      MailIngestionService mailIngestionService,
                                      WorkflowRunService workflowRunService,
                                      WorkflowAnalysisService workflowAnalysisService,
                                      WorkflowEvaluationService workflowEvaluationService,
                                      DemoReviewLoopService demoReviewLoopService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
        this.workflowAnalysisService = workflowAnalysisService;
        this.workflowEvaluationService = workflowEvaluationService;
        this.demoReviewLoopService = demoReviewLoopService;
        this.scenarioMap = loadScenarios(objectMapper);
    }

    public List<DemoScenarioSummaryView> listScenarios() {
        return scenarioMap.values().stream()
                .map(DemoScenarioDefinition::summary)
                .toList();
    }

    public DemoScenarioExecutionView execute(String scenarioId, String mode) {
        DemoScenarioDefinition scenario = requireScenario(scenarioId);
        DemoScenarioMode executeMode = DemoScenarioMode.from(mode);
        Object result = switch (executeMode) {
            case RUN -> runScenario(scenario);
            case ANALYSIS -> workflowAnalysisService.analyze(scenario.toInboundMail());
            case REPLAY -> replayScenario(scenario);
            case REVIEW_LOOP -> demoReviewLoopService.execute(scenario.toInboundMail());
            case VALIDATE -> validateScenario(scenario);
        };
        return new DemoScenarioExecutionView(
                scenario.summary(),
                executeMode.modeName(),
                buildExecutionSummary(scenario, executeMode, result),
                result
        );
    }

    public DemoScenarioSummaryView getScenario(String scenarioId) {
        return requireScenario(scenarioId).summary();
    }

    private WorkflowRun runScenario(DemoScenarioDefinition scenario) {
        return mailIngestionService.process(scenario.toInboundMail());
    }

    private WorkflowReplayView replayScenario(DemoScenarioDefinition scenario) {
        WorkflowRun run = runScenario(scenario);
        return workflowRunService.findReplay(run.getRunId())
                .orElseThrow(() -> new NoSuchElementException("workflow replay not found for runId=" + run.getRunId()));
    }

    private DemoScenarioValidationView validateScenario(DemoScenarioDefinition scenario) {
        DemoScenarioMode validationMode = DemoScenarioMode.from(scenario.metadata().recommendedMode());
        return switch (validationMode) {
            case ANALYSIS -> validateAnalysisScenario(scenario);
            case REPLAY -> validateReplayScenario(scenario);
            case REVIEW_LOOP -> validateReviewLoopScenario(scenario);
            case RUN -> validateRunScenario(scenario);
            case VALIDATE -> throw new IllegalStateException("validate mode cannot recursively validate itself");
        };
    }

    private DemoScenarioValidationView validateAnalysisScenario(DemoScenarioDefinition scenario) {
        WorkflowAnalysisView analysisView = workflowAnalysisService.analyze(scenario.toInboundMail());
        return buildValidationView(
                scenario,
                DemoScenarioMode.ANALYSIS,
                analysisView.routeResult().scene().name(),
                analysisView.routeResult().subIntent().toUpperCase(Locale.ROOT),
                null,
                analysisView.draft().getStatus().name(),
                analysisView.businessFactResult().status().name(),
                mapResultType(analysisView.draft().getStatus().name(), null)
        );
    }

    private DemoScenarioValidationView validateReplayScenario(DemoScenarioDefinition scenario) {
        WorkflowReplayView replayView = replayScenario(scenario);
        WorkflowEvaluationSampleView evaluation = workflowEvaluationService.getSample(replayView.run().getRunId());
        return buildValidationView(
                scenario,
                DemoScenarioMode.REPLAY,
                evaluation.scene(),
                evaluation.subIntent(),
                evaluation.workflowStatus(),
                evaluation.draftStatus(),
                evaluation.businessFactStatus(),
                mapResultType(evaluation.draftStatus(), replayView.run().getStatus().name())
        );
    }

    private DemoScenarioValidationView validateReviewLoopScenario(DemoScenarioDefinition scenario) {
        DemoReviewLoopExecutionView reviewLoop = demoReviewLoopService.execute(scenario.toInboundMail());
        WorkflowEvaluationSampleView evaluation = reviewLoop.evaluation();
        return buildValidationView(
                scenario,
                DemoScenarioMode.REVIEW_LOOP,
                evaluation.scene(),
                evaluation.subIntent(),
                evaluation.workflowStatus(),
                reviewLoop.initialDraft().draftStatus(),
                evaluation.businessFactStatus(),
                mapResultType(reviewLoop.initialDraft().draftStatus(), reviewLoop.run().getStatus().name())
        );
    }

    private DemoScenarioValidationView validateRunScenario(DemoScenarioDefinition scenario) {
        WorkflowRun run = runScenario(scenario);
        WorkflowEvaluationSampleView evaluation = workflowEvaluationService.getSample(run.getRunId());
        return buildValidationView(
                scenario,
                DemoScenarioMode.RUN,
                evaluation.scene(),
                evaluation.subIntent(),
                evaluation.workflowStatus(),
                evaluation.draftStatus(),
                evaluation.businessFactStatus(),
                mapResultType(evaluation.draftStatus(), run.getStatus().name())
        );
    }

    private DemoScenarioValidationView buildValidationView(DemoScenarioDefinition scenario,
                                                           DemoScenarioMode mode,
                                                           String actualScene,
                                                           String actualWorkflowSubIntent,
                                                           String actualWorkflowStatus,
                                                           String actualDraftStatus,
                                                           String actualBusinessFactStatus,
                                                           String actualResultType) {
        List<DemoScenarioValidationCheckView> checks = new java.util.ArrayList<>();
        addCheck(checks, "scene", scenario.metadata().expectedWorkflowScene(), actualScene);
        addCheck(checks, "workflowSubIntent", scenario.metadata().expectedWorkflowSubIntent(), actualWorkflowSubIntent);
        addCheck(checks, "workflowStatus", scenario.metadata().expectedWorkflowStatus(), actualWorkflowStatus);
        addCheck(checks, "draftStatus", scenario.metadata().expectedDraftStatus(), actualDraftStatus);
        addCheck(checks, "businessFactStatus", scenario.metadata().expectedBusinessFactStatus(), actualBusinessFactStatus);
        addCheck(checks, "resultType", scenario.metadata().expectedResultType(), actualResultType);
        boolean passed = checks.stream().allMatch(DemoScenarioValidationCheckView::passed);
        return new DemoScenarioValidationView(scenario.metadata().scenarioId(), mode.modeName(), passed, checks);
    }

    private void addCheck(List<DemoScenarioValidationCheckView> checks, String key, String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return;
        }
        String actualValue = actual == null || actual.isBlank() ? "UNKNOWN" : actual;
        checks.add(new DemoScenarioValidationCheckView(key, expected, actualValue, expected.equalsIgnoreCase(actualValue)));
    }

    private String mapResultType(String draftStatus, String workflowStatus) {
        if ("BLOCKED".equalsIgnoreCase(workflowStatus)) {
            return "阻断";
        }
        if ("FOLLOW_UP_NEEDED".equalsIgnoreCase(draftStatus)) {
            return "先追问";
        }
        if ("HUMAN_REVIEW_REQUIRED".equalsIgnoreCase(draftStatus)) {
            return "人工审核";
        }
        return "直接草稿";
    }

    private DemoScenarioExecutionSummaryView buildExecutionSummary(DemoScenarioDefinition scenario,
                                                                  DemoScenarioMode executeMode,
                                                                  Object result) {
        return switch (executeMode) {
            case ANALYSIS -> buildAnalysisExecutionSummary(scenario, (WorkflowAnalysisView) result);
            case REPLAY -> buildReplayExecutionSummary(scenario, (WorkflowReplayView) result);
            case REVIEW_LOOP -> buildReviewLoopExecutionSummary(scenario, (DemoReviewLoopExecutionView) result);
            case RUN -> buildRunExecutionSummary(scenario, (WorkflowRun) result);
            case VALIDATE -> buildValidationExecutionSummary(scenario, (DemoScenarioValidationView) result);
        };
    }

    private DemoScenarioExecutionSummaryView buildAnalysisExecutionSummary(DemoScenarioDefinition scenario,
                                                                          WorkflowAnalysisView analysisView) {
        WorkflowAnalysisSummaryView summary = analysisView.summary();
        return new DemoScenarioExecutionSummaryView(
                "ANALYSIS_PREVIEW",
                "analysis",
                scenario.metadata().demoFocus(),
                summary.scene(),
                summary.subIntent(),
                mapResultType(analysisView.draft().getStatus().name(), null),
                "ANALYSIS_PREVIEW",
                analysisView.draft().getStatus().name(),
                mapAnalysisRiskLevel(summary.finalStatus()),
                mapAnalysisReleaseDecision(summary.operatorDecision()),
                "dispatch_reply".equals(summary.nextAction()),
                summary.operatorDecision(),
                summary.nextAction(),
                analysisView.businessFactResult().status().name(),
                summary.factSummary(),
                summary.knowledgeSummary(),
                summary.replySummary(),
                mergeEvidence(
                        List.of(
                                "scenario=" + scenario.metadata().scenarioId(),
                                "business_hint=" + scenario.metadata().businessEvidenceHint(),
                                "knowledge_hint=" + scenario.metadata().knowledgeEvidenceHint()
                        ),
                        summary.keyEvidence()
                )
        );
    }

    private DemoScenarioExecutionSummaryView buildReplayExecutionSummary(DemoScenarioDefinition scenario,
                                                                        WorkflowReplayView replayView) {
        WorkflowEvaluationSampleView evaluation = workflowEvaluationService.getSample(replayView.run().getRunId());
        return buildEvaluationBackedSummary(
                scenario,
                "replay",
                replayView.run().getRunId(),
                evaluation,
                mapResultType(evaluation.draftStatus(), replayView.run().getStatus().name()),
                deriveOperatorDecision(evaluation),
                defaultText(evaluation.nextAction(), "inspect_replay"),
                buildReplyEvidence(evaluation),
                mergeEvidence(
                        List.of("scenario=" + scenario.metadata().scenarioId()),
                        extractEvaluationEvidence(evaluation)
                )
        );
    }

    private DemoScenarioExecutionSummaryView buildReviewLoopExecutionSummary(DemoScenarioDefinition scenario,
                                                                            DemoReviewLoopExecutionView reviewLoopView) {
        WorkflowEvaluationSampleView evaluation = reviewLoopView.evaluation();
        return buildEvaluationBackedSummary(
                scenario,
                "review_loop",
                reviewLoopView.run().getRunId(),
                evaluation,
                "人工审核闭环",
                "manual_review_completed",
                defaultText(reviewLoopView.approvedDraft().nextAction(), "dispatch_reply"),
                buildReviewLoopReplyEvidence(reviewLoopView),
                mergeEvidence(
                        List.of("scenario=" + scenario.metadata().scenarioId()),
                        extractReviewLoopEvidence(reviewLoopView)
                )
        );
    }

    private DemoScenarioExecutionSummaryView buildRunExecutionSummary(DemoScenarioDefinition scenario,
                                                                     WorkflowRun run) {
        WorkflowEvaluationSampleView evaluation = workflowEvaluationService.getSample(run.getRunId());
        return buildEvaluationBackedSummary(
                scenario,
                "run",
                run.getRunId(),
                evaluation,
                mapResultType(evaluation.draftStatus(), run.getStatus().name()),
                deriveOperatorDecision(evaluation),
                defaultText(evaluation.nextAction(), "inspect_run"),
                buildReplyEvidence(evaluation),
                mergeEvidence(
                        List.of("scenario=" + scenario.metadata().scenarioId()),
                        extractEvaluationEvidence(evaluation)
                )
        );
    }

    private DemoScenarioExecutionSummaryView buildValidationExecutionSummary(DemoScenarioDefinition scenario,
                                                                            DemoScenarioValidationView validationView) {
        List<String> keyEvidence = validationView.checks().stream()
                .map(check -> (check.passed() ? "passed_check=" : "failed_check=") + check.key()
                        + ":" + check.actual())
                .limit(6)
                .toList();
        return new DemoScenarioExecutionSummaryView(
                "VALIDATE_ONLY",
                "validate",
                scenario.metadata().demoFocus(),
                defaultText(scenario.metadata().expectedWorkflowScene(), scenario.metadata().scene()),
                defaultText(scenario.metadata().expectedWorkflowSubIntent(), scenario.metadata().subIntent()),
                validationView.passed() ? "校验通过" : "校验失败",
                validationView.validatedMode().toUpperCase(Locale.ROOT),
                defaultText(scenario.metadata().expectedDraftStatus(), "UNKNOWN"),
                validationView.passed() ? "LOW" : "MEDIUM",
                validationView.passed() ? "EXPECTATION_CONFIRMED" : "EXPECTATION_MISMATCH",
                validationView.passed(),
                validationView.passed() ? "scenario_expectation_matched" : "scenario_expectation_mismatch",
                validationView.passed() ? "continue_demo" : "inspect_failed_checks",
                defaultText(scenario.metadata().expectedBusinessFactStatus(), "UNKNOWN"),
                scenario.metadata().businessEvidenceHint(),
                scenario.metadata().knowledgeEvidenceHint(),
                buildValidationReplyEvidence(scenario, validationView),
                keyEvidence
        );
    }

    private DemoScenarioExecutionSummaryView buildEvaluationBackedSummary(DemoScenarioDefinition scenario,
                                                                         String mode,
                                                                         String runId,
                                                                         WorkflowEvaluationSampleView evaluation,
                                                                         String resultType,
                                                                         String operatorDecision,
                                                                         String nextAction,
                                                                         String replyEvidence,
                                                                         List<String> keyEvidence) {
        return new DemoScenarioExecutionSummaryView(
                runId,
                mode,
                scenario.metadata().demoFocus(),
                evaluation.scene(),
                evaluation.subIntent(),
                resultType,
                evaluation.workflowStatus(),
                defaultText(evaluation.draftStatus(), "UNKNOWN"),
                evaluation.riskDecision().riskLevel(),
                evaluation.riskDecision().releaseDecision(),
                evaluation.riskDecision().sendAllowed(),
                operatorDecision,
                nextAction,
                evaluation.businessFactStatus(),
                evaluation.businessFactsSummary(),
                evaluation.knowledgeSummary(),
                replyEvidence,
                keyEvidence
        );
    }

    private String mapAnalysisRiskLevel(String finalStatus) {
        return switch (finalStatus) {
            case "BLOCKED" -> "CRITICAL";
            case "HUMAN_REVIEW_REQUIRED" -> "HIGH";
            case "FOLLOW_UP_NEEDED" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private String mapAnalysisReleaseDecision(String operatorDecision) {
        return switch (operatorDecision) {
            case "blocked" -> "BLOCKED";
            case "manual_review_required" -> "HOLD_FOR_REVIEW";
            case "follow_up_required" -> "NEED_CUSTOMER_FOLLOW_UP";
            case "direct_reply_ready" -> "READY_FOR_DISPATCH";
            case "review_passed_waiting_dispatch" -> "DRAFT_READY";
            default -> "OBSERVE";
        };
    }

    private String deriveOperatorDecision(WorkflowEvaluationSampleView evaluation) {
        if ("BLOCKED".equalsIgnoreCase(evaluation.workflowStatus())) {
            return "blocked";
        }
        if (evaluation.manualReviewOutcome() != null
                && !evaluation.manualReviewOutcome().isBlank()
                && !"NOT_REVIEWED".equalsIgnoreCase(evaluation.manualReviewOutcome())) {
            return "manual_review_completed";
        }
        if ("FOLLOW_UP_NEEDED".equalsIgnoreCase(evaluation.draftStatus())) {
            return "follow_up_required";
        }
        if ("HUMAN_REVIEW_REQUIRED".equalsIgnoreCase(evaluation.draftStatus())) {
            return "manual_review_required";
        }
        if ("READY_FOR_SEND".equalsIgnoreCase(evaluation.sendReadiness())) {
            return "ready_for_dispatch";
        }
        if (evaluation.latestDispatchStatus() != null && !evaluation.latestDispatchStatus().isBlank()) {
            return "dispatch_recorded";
        }
        return "draft_generated";
    }

    private String buildReplyEvidence(WorkflowEvaluationSampleView evaluation) {
        StringBuilder builder = new StringBuilder();
        builder.append("replySource=").append(evaluation.replySource());
        if (evaluation.replyFallbackReason() != null && !evaluation.replyFallbackReason().isBlank()) {
            builder.append(", fallback=").append(evaluation.replyFallbackReason());
        }
        if (evaluation.sendReadiness() != null && !evaluation.sendReadiness().isBlank()) {
            builder.append(", sendReadiness=").append(evaluation.sendReadiness());
        }
        if (evaluation.latestDispatchStatus() != null && !evaluation.latestDispatchStatus().isBlank()) {
            builder.append(", dispatchStatus=").append(evaluation.latestDispatchStatus());
        }
        return builder.toString();
    }

    private String buildReviewLoopReplyEvidence(DemoReviewLoopExecutionView reviewLoopView) {
        return "initialDraft=" + reviewLoopView.initialDraft().draftStatus()
                + ", approvedDraft=" + reviewLoopView.approvedDraft().draftStatus()
                + ", sendReadiness=" + reviewLoopView.approvedDraft().sendReadiness()
                + ", latestReviewAction=" + reviewLoopView.evaluation().latestReviewAction();
    }

    private String buildValidationReplyEvidence(DemoScenarioDefinition scenario,
                                                DemoScenarioValidationView validationView) {
        return "validatedMode=" + validationView.validatedMode()
                + ", expectedResultType=" + scenario.metadata().expectedResultType()
                + ", passed=" + validationView.passed();
    }

    private List<String> extractEvaluationEvidence(WorkflowEvaluationSampleView evaluation) {
        return mergeEvidence(
                evaluation.businessFactSourceSystems().stream()
                        .limit(2)
                        .map(value -> "fact_source=" + value)
                        .toList(),
                mergeEvidence(
                        evaluation.knowledgeSnippetIds().stream()
                                .limit(3)
                                .map(value -> "knowledge_snippet=" + value)
                                .toList(),
                        mergeEvidence(
                                evaluation.riskFlags().stream()
                                        .limit(2)
                                        .map(value -> "risk_flag=" + value)
                                        .toList(),
                                evaluation.latestReviewFeedbackTags().stream()
                                        .limit(2)
                                        .map(value -> "review_tag=" + value)
                                        .toList()
                        )
                )
        );
    }

    private List<String> extractReviewLoopEvidence(DemoReviewLoopExecutionView reviewLoopView) {
        WorkflowEvaluationSampleView evaluation = reviewLoopView.evaluation();
        return mergeEvidence(
                List.of(
                        "review_count=" + evaluation.reviewCount(),
                        "revision_count=" + evaluation.revisionCount(),
                        "manual_review_outcome=" + evaluation.manualReviewOutcome()
                ),
                extractEvaluationEvidence(evaluation)
        );
    }

    private List<String> mergeEvidence(List<String> primary, List<String> secondary) {
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        if (primary != null) {
            merged.addAll(primary.stream().filter(value -> value != null && !value.isBlank()).toList());
        }
        if (secondary != null) {
            merged.addAll(secondary.stream().filter(value -> value != null && !value.isBlank()).toList());
        }
        return List.copyOf(merged);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private DemoScenarioDefinition requireScenario(String scenarioId) {
        DemoScenarioDefinition scenario = scenarioMap.get(scenarioId);
        if (scenario == null) {
            throw new NoSuchElementException("demo scenario not found: " + scenarioId);
        }
        return scenario;
    }

    private Map<String, DemoScenarioDefinition> loadScenarios(ObjectMapper objectMapper) {
        LinkedHashMap<String, DemoScenarioDefinition> scenarios = new LinkedHashMap<>();
        for (DemoScenarioMetadata metadata : DemoScenarioMetadata.defaults()) {
            DemoScenarioPayload payload = readPayload(metadata.scenarioId(), objectMapper);
            scenarios.put(metadata.scenarioId(), new DemoScenarioDefinition(metadata, payload));
        }
        return Collections.unmodifiableMap(scenarios);
    }

    private DemoScenarioPayload readPayload(String scenarioId, ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("demo-scenarios/" + scenarioId + ".json");
        if (!resource.exists()) {
            throw new IllegalStateException("missing demo scenario resource: " + scenarioId);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, DemoScenarioPayload.class);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to load demo scenario: " + scenarioId, exception);
        }
    }

    private enum DemoScenarioMode {
        RUN,
        ANALYSIS,
        REPLAY,
        REVIEW_LOOP,
        VALIDATE;

        static DemoScenarioMode from(String mode) {
            if (mode == null || mode.isBlank()) {
                return RUN;
            }
            try {
                return DemoScenarioMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("unsupported demo scenario mode: " + mode);
            }
        }

        String modeName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private record DemoScenarioDefinition(
            DemoScenarioMetadata metadata,
            DemoScenarioPayload payload
    ) {
        DemoScenarioSummaryView summary() {
            return new DemoScenarioSummaryView(
                    metadata.scenarioId(),
                    metadata.title(),
                    metadata.scene(),
                    metadata.subIntent(),
                    metadata.description(),
                    payload.from(),
                    payload.subject(),
                    metadata.recommendedMode(),
                    metadata.demoFocus(),
                    metadata.expectedResultType(),
                    metadata.businessEvidenceHint(),
                    metadata.knowledgeEvidenceHint()
            );
        }

        InboundMail toInboundMail() {
            return new InboundMail(
                    payload.messageId(),
                    payload.threadId(),
                    payload.from(),
                    payload.subject(),
                    payload.body(),
                    OffsetDateTime.now()
            );
        }
    }

    private record DemoScenarioPayload(
            String messageId,
            String threadId,
            String from,
            String subject,
            String body
    ) {
    }

    private record DemoScenarioMetadata(
            String scenarioId,
            String title,
            String scene,
            String subIntent,
            String description,
            String recommendedMode,
            String demoFocus,
            String expectedResultType,
            String businessEvidenceHint,
            String knowledgeEvidenceHint,
            String expectedWorkflowScene,
            String expectedWorkflowSubIntent,
            String expectedWorkflowStatus,
            String expectedDraftStatus,
            String expectedBusinessFactStatus
    ) {
        static List<DemoScenarioMetadata> defaults() {
            return List.of(
                    new DemoScenarioMetadata(
                            "pre-sales-recommendation",
                            "售前推荐样例",
                            "PRE_SALES",
                            "PRE_SALES_RECOMMENDATION",
                            "客户咨询产品推荐方向，验证售前意图识别、知识召回和推荐草稿生成。",
                            "analysis",
                            "售前推荐方向识别与知识补充",
                            "直接草稿",
                            "这类样例通常不依赖订单类事实，重点确认 facts 是否明确标记为 NOT_REQUIRED。",
                            "重点看知识召回是否补上推荐方向、适用空间和基础说明。",
                            "PRE_SALES",
                            "PRODUCT_RECOMMENDATION",
                            null,
                            "DRAFT_READY",
                            "NOT_REQUIRED"
                    ),
                    new DemoScenarioMetadata(
                            "pre-sales-comparison",
                            "售前对比样例",
                            "PRE_SALES",
                            "PRE_SALES_COMPARISON",
                            "客户咨询两个产品方向的差异，验证对比说明与知识召回。",
                            "analysis",
                            "售前对比说明与知识片段支撑",
                            "直接草稿",
                            "一般不依赖业务 facts，重点确认 facts 不会误触发售后查询。",
                            "重点看知识是否给出差异点、适用建议和对比依据。",
                            "PRE_SALES",
                            "PRODUCT_COMPARISON",
                            null,
                            "DRAFT_READY",
                            "NOT_REQUIRED"
                    ),
                    new DemoScenarioMetadata(
                            "pre-sales-general-inquiry",
                            "售前基础功能咨询样例",
                            "PRE_SALES",
                            "PRE_SALES_GENERAL_INQUIRY",
                            "客户咨询基础功能与使用方式，验证售前 general inquiry 路由与回复。",
                            "analysis",
                            "售前泛咨询路由与基础说明",
                            "直接草稿",
                            "通常不依赖订单或物流 facts，重点确认不会被误判为售后。",
                            "重点看知识是否补足功能说明、使用前提和常见限制。",
                            "PRE_SALES",
                            "GENERAL_INQUIRY",
                            null,
                            "DRAFT_READY",
                            "NOT_REQUIRED"
                    ),
                    new DemoScenarioMetadata(
                            "pre-sales-shipping-stock",
                            "售前库存发货样例",
                            "PRE_SALES",
                            "PRE_SALES_INVENTORY_OR_SHIPPING",
                            "客户咨询库存和跨境发货时效，验证 inventory_or_shipping 子意图。",
                            "analysis",
                            "售前库存与发货咨询",
                            "直接草稿",
                            "一般不查售后订单事实，重点确认不会误走 order_status 或 logistics_tracking。",
                            "重点看知识是否补足库存说明、发货时效和跨境预期。",
                            "PRE_SALES",
                            "INVENTORY_OR_SHIPPING",
                            null,
                            "DRAFT_READY",
                            "NOT_REQUIRED"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-order-status",
                            "售后订单状态样例",
                            "AFTER_SALES",
                            "AFTER_SALES_ORDER_STATUS",
                            "客户提供订单号并询问当前订单状态，验证 order_status 子意图和订单 facts。",
                            "replay",
                            "售后订单状态 facts-first 演示",
                            "直接草稿",
                            "优先确认订单 facts 是否命中当前订单状态、发货阶段和关键实体解析。",
                            "重点看知识是否只做状态解释、时效预期和非承诺性补充。",
                            "AFTER_SALES",
                            "ORDER_STATUS",
                            "COMPLETED",
                            "DRAFT_READY",
                            "SUCCESS"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-manual-review",
                            "售后人工审核样例",
                            "AFTER_SALES",
                            "AFTER_SALES_MANUAL_REVIEW",
                            "客户要求退款与赔付，验证高风险请求进入人工审核。",
                            "review_loop",
                            "高风险售后进入人工审核并回流修改",
                            "人工审核",
                            "先确认 facts 是否核验了订单上下文，再看高风险诉求如何抬升审核要求。",
                            "重点看知识如何补政策措辞与保守说明，而不是直接做赔付承诺。",
                            "AFTER_SALES",
                            "RETURN_REFUND",
                            "COMPLETED",
                            "HUMAN_REVIEW_REQUIRED",
                            "SUCCESS"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-logistics",
                            "售后物流查询样例",
                            "AFTER_SALES",
                            "AFTER_SALES_LOGISTICS",
                            "客户提供订单号和物流号，验证售后事实查询与物流说明回复。",
                            "replay",
                            "售后物流查询的 facts 与 knowledge 协同",
                            "直接草稿",
                            "优先看订单和物流 facts；如果出现冲突或无结果，也要能在 replay / evaluation 里直接看出来。",
                            "重点看知识是否补物流节点解释、时效预期和保守措辞。",
                            "AFTER_SALES",
                            "LOGISTICS_TRACKING",
                            "COMPLETED",
                            "DRAFT_READY",
                            "CONFLICT"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-policy",
                            "售后政策说明样例",
                            "AFTER_SALES",
                            "AFTER_SALES_POLICY",
                            "客户咨询退款或退换政策，验证售后政策解释与风险保守回复。",
                            "analysis",
                            "售后政策说明与 facts-before-knowledge",
                            "直接草稿",
                            "如果缺少明确订单实体，facts 可能命中不足，但仍要说明是否已尝试核验订单上下文。",
                            "重点看知识是否补政策边界、处理步骤和风险保守表达。",
                            "AFTER_SALES",
                            "AFTER_SALES_POLICY",
                            null,
                            "DRAFT_READY",
                            "SUCCESS"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-missing-id",
                            "售后缺编号追问样例",
                            "AFTER_SALES",
                            "AFTER_SALES_MISSING_IDENTIFIER",
                            "客户表达售后诉求但缺少订单号或物流号，验证补充追问链路。",
                            "analysis",
                            "售后缺关键编号时的追问兜底",
                            "先追问",
                            "重点确认 facts 是否明确给出 INSUFFICIENT_INPUT，而不是假装查到了结果。",
                            "知识只能补通用说明，不能替代缺失的订单号或物流号。",
                            "AFTER_SALES",
                            null,
                            null,
                            "FOLLOW_UP_NEEDED",
                            "INSUFFICIENT_INPUT"
                    ),
                    new DemoScenarioMetadata(
                            "system-blocked-demo",
                            "系统阻断样例",
                            "SYSTEM",
                            "BLOCKED_DEMO",
                            "仅用于本地演示系统级故障进入 BLOCKED 状态，不代表真实业务子意图。",
                            "replay",
                            "系统级阻断路径演示",
                            "阻断",
                            "这类样例不是业务 facts 查询失败，而是系统层面主动阻断主链路。",
                            "这类样例不以 knowledge 补充为重点，重点是说明为什么必须阻断。",
                            null,
                            null,
                            "BLOCKED",
                            null,
                            null
                    )
            );
        }
    }
}
