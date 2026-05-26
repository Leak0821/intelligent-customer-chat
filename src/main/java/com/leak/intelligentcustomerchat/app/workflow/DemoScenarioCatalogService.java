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
    private final Map<String, DemoScenarioDefinition> scenarioMap;

    public DemoScenarioCatalogService(ObjectMapper objectMapper,
                                      MailIngestionService mailIngestionService,
                                      WorkflowRunService workflowRunService,
                                      WorkflowAnalysisService workflowAnalysisService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
        this.workflowAnalysisService = workflowAnalysisService;
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
        REPLAY;

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
                    payload.subject()
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
            String description
    ) {
        static List<DemoScenarioMetadata> defaults() {
            return List.of(
                    new DemoScenarioMetadata(
                            "pre-sales-recommendation",
                            "售前推荐样例",
                            "PRE_SALES",
                            "PRE_SALES_RECOMMENDATION",
                            "客户咨询产品推荐方向，验证售前意图识别、知识召回和推荐草稿生成。"
                    ),
                    new DemoScenarioMetadata(
                            "pre-sales-comparison",
                            "售前对比样例",
                            "PRE_SALES",
                            "PRE_SALES_COMPARISON",
                            "客户咨询两个产品方向的差异，验证对比说明与知识召回。"
                    ),
                    new DemoScenarioMetadata(
                            "pre-sales-general-inquiry",
                            "售前基础功能咨询样例",
                            "PRE_SALES",
                            "PRE_SALES_GENERAL_INQUIRY",
                            "客户咨询基础功能与使用方式，验证售前 general inquiry 路由与回复。"
                    ),
                    new DemoScenarioMetadata(
                            "pre-sales-shipping-stock",
                            "售前库存发货样例",
                            "PRE_SALES",
                            "PRE_SALES_INVENTORY_OR_SHIPPING",
                            "客户咨询库存和跨境发货时效，验证 inventory_or_shipping 子意图。"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-order-status",
                            "售后订单状态样例",
                            "AFTER_SALES",
                            "AFTER_SALES_ORDER_STATUS",
                            "客户提供订单号并询问当前订单状态，验证 order_status 子意图和订单 facts。"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-logistics",
                            "售后物流查询样例",
                            "AFTER_SALES",
                            "AFTER_SALES_LOGISTICS",
                            "客户提供订单号和物流号，验证售后事实查询与物流说明回复。"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-policy",
                            "售后政策说明样例",
                            "AFTER_SALES",
                            "AFTER_SALES_POLICY",
                            "客户咨询退款或退换政策，验证售后政策解释与风险保守回复。"
                    ),
                    new DemoScenarioMetadata(
                            "after-sales-missing-id",
                            "售后缺编号追问样例",
                            "AFTER_SALES",
                            "AFTER_SALES_MISSING_IDENTIFIER",
                            "客户表达售后诉求但缺少订单号或物流号，验证补充追问链路。"
                    )
            );
        }
    }
}
