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
    private final DemoReviewLoopService demoReviewLoopService;
    private final Map<String, DemoScenarioDefinition> scenarioMap;

    public DemoScenarioCatalogService(ObjectMapper objectMapper,
                                      MailIngestionService mailIngestionService,
                                      WorkflowRunService workflowRunService,
                                      WorkflowAnalysisService workflowAnalysisService,
                                      DemoReviewLoopService demoReviewLoopService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
        this.workflowAnalysisService = workflowAnalysisService;
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
        };
        return new DemoScenarioExecutionView(scenario.summary(), executeMode.modeName(), result);
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
        REVIEW_LOOP;

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
            String knowledgeEvidenceHint
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
                            "重点看知识召回是否补上推荐方向、适用空间和基础说明。"
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
                            "重点看知识是否给出差异点、适用建议和对比依据。"
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
                            "重点看知识是否补足功能说明、使用前提和常见限制。"
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
                            "重点看知识是否补足库存说明、发货时效和跨境预期。"
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
                            "重点看知识是否只做状态解释、时效预期和非承诺性补充。"
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
                            "重点看知识如何补政策措辞与保守说明，而不是直接做赔付承诺。"
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
                            "重点看知识是否补物流节点解释、时效预期和保守措辞。"
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
                            "重点看知识是否补政策边界、处理步骤和风险保守表达。"
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
                            "知识只能补通用说明，不能替代缺失的订单号或物流号。"
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
                            "这类样例不以 knowledge 补充为重点，重点是说明为什么必须阻断。"
                    )
            );
        }
    }
}
