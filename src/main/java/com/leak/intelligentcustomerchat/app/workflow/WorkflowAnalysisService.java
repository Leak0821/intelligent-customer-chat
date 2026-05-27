package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.config.IntentConfigService;
import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingDiagnostics;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingTraceService;
import com.leak.intelligentcustomerchat.app.intent.IntentHeuristicPreviewService;
import com.leak.intelligentcustomerchat.app.intent.ContextAwareIntentNormalizationResult;
import com.leak.intelligentcustomerchat.app.intent.ContextAwareIntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationDiagnostics;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationTraceService;
import com.leak.intelligentcustomerchat.app.intent.IntentRoutingService;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrievalQueryBuilder;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrieveService;
import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftingDiagnostics;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftingResult;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionContext;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionService;
import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummaryRepository;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.HybridRetrievalResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.HybridSearchService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class WorkflowAnalysisService {
    private final MailCleaner mailCleaner;
    private final IntentHeuristicPreviewService intentHeuristicPreviewService;
    private final IntentNormalizationService intentNormalizationService;
    private final ObjectProvider<IntentNormalizationTraceService> intentNormalizationTraceServiceProvider;
    private final ObjectProvider<ContextLoadingTraceService> contextLoadingTraceServiceProvider;
    private final IntentRoutingService intentRoutingService;
    private final ContextLoadingService contextLoadingService;
    private final ContextAwareIntentNormalizationService contextAwareIntentNormalizationService;
    private final BusinessFactService businessFactService;
    private final KnowledgeRetrievalQueryBuilder knowledgeRetrievalQueryBuilder;
    private final KnowledgeRetrieveService knowledgeRetrieveService;
    private final ReplyDraftService replyDraftService;
    private final ReviewDecisionService reviewDecisionService;
    private final IntentConfigService intentConfigService;
    private final RetrievalConfigService retrievalConfigService;
    private final ConversationMemoryStore conversationMemoryStore;
    private final ConversationSummaryRepository conversationSummaryRepository;
    private final ContextMemoryProperties contextMemoryProperties;
    private final ObjectProvider<HybridSearchService> hybridSearchServiceProvider;

    public WorkflowAnalysisService(MailCleaner mailCleaner,
                                   IntentHeuristicPreviewService intentHeuristicPreviewService,
                                   IntentNormalizationService intentNormalizationService,
                                   ObjectProvider<IntentNormalizationTraceService> intentNormalizationTraceServiceProvider,
                                   ObjectProvider<ContextLoadingTraceService> contextLoadingTraceServiceProvider,
                                   IntentRoutingService intentRoutingService,
                                   ContextLoadingService contextLoadingService,
                                   ContextAwareIntentNormalizationService contextAwareIntentNormalizationService,
                                   BusinessFactService businessFactService,
                                   KnowledgeRetrievalQueryBuilder knowledgeRetrievalQueryBuilder,
                                   KnowledgeRetrieveService knowledgeRetrieveService,
                                   ReplyDraftService replyDraftService,
                                   ReviewDecisionService reviewDecisionService,
                                   IntentConfigService intentConfigService,
                                   RetrievalConfigService retrievalConfigService,
                                   ConversationMemoryStore conversationMemoryStore,
                                   ConversationSummaryRepository conversationSummaryRepository,
                                   ContextMemoryProperties contextMemoryProperties,
                                   ObjectProvider<HybridSearchService> hybridSearchServiceProvider) {
        this.mailCleaner = mailCleaner;
        this.intentHeuristicPreviewService = intentHeuristicPreviewService;
        this.intentNormalizationService = intentNormalizationService;
        this.intentNormalizationTraceServiceProvider = intentNormalizationTraceServiceProvider;
        this.contextLoadingTraceServiceProvider = contextLoadingTraceServiceProvider;
        this.intentRoutingService = intentRoutingService;
        this.contextLoadingService = contextLoadingService;
        this.contextAwareIntentNormalizationService = contextAwareIntentNormalizationService;
        this.businessFactService = businessFactService;
        this.knowledgeRetrievalQueryBuilder = knowledgeRetrievalQueryBuilder;
        this.knowledgeRetrieveService = knowledgeRetrieveService;
        this.replyDraftService = replyDraftService;
        this.reviewDecisionService = reviewDecisionService;
        this.intentConfigService = intentConfigService;
        this.retrievalConfigService = retrievalConfigService;
        this.conversationMemoryStore = conversationMemoryStore;
        this.conversationSummaryRepository = conversationSummaryRepository;
        this.contextMemoryProperties = contextMemoryProperties;
        this.hybridSearchServiceProvider = hybridSearchServiceProvider;
    }

    public WorkflowAnalysisView analyze(InboundMail inboundMail) {
        InboundMail cleanedMail = mailCleaner.clean(inboundMail);
        IntentNormalizationDiagnostics normalizationDiagnostics = diagnoseIntent(cleanedMail);
        IntentNormalizationResult heuristicBaseline = normalizationDiagnostics.heuristicBaseline();
        IntentNormalizationResult normalizationResult = normalizationDiagnostics.finalResult();
        IntentRouteResult routeResult = intentRoutingService.route(normalizationResult);
        ContextLoadingDiagnostics contextLoadingDiagnostics = diagnoseContext(cleanedMail, routeResult);
        ContextSnapshot contextSnapshot = contextLoadingDiagnostics.snapshot();
        ContextAwareIntentNormalizationResult contextAwareNormalization = contextAwareIntentNormalizationService.enrich(normalizationResult, contextSnapshot);
        IntentNormalizationResult effectiveNormalizationResult = contextAwareNormalization.result();
        BusinessFactResult businessFactResult = businessFactService.loadFacts(cleanedMail, effectiveNormalizationResult, routeResult, contextSnapshot);
        RetrievalQuery retrievalQuery = buildRetrievalQuery(effectiveNormalizationResult, routeResult, contextSnapshot, businessFactResult);
        KnowledgeRetrieveResult knowledgeRetrieveResult = knowledgeRetrieveService.retrieve(effectiveNormalizationResult, routeResult, contextSnapshot, businessFactResult);

        // 分析视图不落正式状态机，只复用同一套草稿与审核能力，方便快速检查中间产物。
        WorkflowRun previewRun = WorkflowRun.start(cleanedMail.messageId(), cleanedMail.threadId());
        ReplyDraftingResult draftingResult = replyDraftService.draftWithDiagnostics(
                previewRun,
                cleanedMail,
                effectiveNormalizationResult,
                routeResult,
                contextSnapshot,
                businessFactResult,
                knowledgeRetrieveResult
        );
        ReplyDraft draft = draftingResult.draft();
        ReviewDecision reviewDecision = reviewDecisionService.review(
                draft,
                new ReviewDecisionContext(routeResult, businessFactResult, knowledgeRetrieveResult)
        );

        return new WorkflowAnalysisView(
                cleanedMail,
                new WorkflowIntentDiagnosticsView(
                        heuristicBaseline,
                        intentConfigService.currentIntentCatalog(),
                        !heuristicBaseline.equals(normalizationResult),
                        normalizationDiagnostics.normalizationSource(),
                        normalizationDiagnostics.llmAttempted(),
                        normalizationDiagnostics.llmResponseAccepted(),
                        normalizationDiagnostics.fallbackReason(),
                        mergeActions(normalizationDiagnostics.guardrailActions(), contextAwareNormalization.actions()),
                        normalizationDiagnostics.heuristicMatchedSignals()
                ),
                effectiveNormalizationResult,
                routeResult,
                contextSnapshot,
                buildContextDiagnostics(cleanedMail.threadId(), contextLoadingDiagnostics),
                buildBusinessFactDiagnostics(routeResult, businessFactResult),
                businessFactResult,
                buildKnowledgeDiagnostics(retrievalQuery, routeResult, businessFactResult, knowledgeRetrieveResult),
                knowledgeRetrieveResult,
                buildReplyDiagnostics(draftingResult.diagnostics()),
                draft,
                reviewDecision
        );
    }

    private IntentNormalizationDiagnostics diagnoseIntent(InboundMail cleanedMail) {
        IntentNormalizationTraceService traceService = intentNormalizationTraceServiceProvider.getIfAvailable();
        if (traceService != null) {
            return traceService.diagnose(cleanedMail);
        }
        IntentNormalizationResult heuristicBaseline = intentHeuristicPreviewService.preview(cleanedMail);
        IntentNormalizationResult finalResult = intentNormalizationService.normalize(cleanedMail);
        return new IntentNormalizationDiagnostics(
                heuristicBaseline,
                finalResult,
                "unknown",
                false,
                false,
                "trace_service_unavailable",
                List.of(),
                List.of()
        );
    }

    private ContextLoadingDiagnostics diagnoseContext(InboundMail cleanedMail, IntentRouteResult routeResult) {
        ContextLoadingTraceService traceService = contextLoadingTraceServiceProvider.getIfAvailable();
        if (traceService != null) {
            return traceService.diagnose(cleanedMail, routeResult);
        }
        ContextSnapshot snapshot = contextLoadingService.load(cleanedMail, routeResult);
        return new ContextLoadingDiagnostics(
                snapshot,
                conversationMemoryStore.totalMessageCount(cleanedMail.threadId()),
                conversationMemoryStore.recentMessages(cleanedMail.threadId()).size(),
                false,
                false,
                "unknown",
                "trace_service_unavailable",
                "unknown",
                false
        );
    }

    private RetrievalQuery buildRetrievalQuery(IntentNormalizationResult normalizationResult,
                                               IntentRouteResult routeResult,
                                               ContextSnapshot contextSnapshot,
                                               BusinessFactResult businessFactResult) {
        return knowledgeRetrievalQueryBuilder.build(
                normalizationResult,
                routeResult,
                contextSnapshot,
                businessFactResult,
                retrievalConfigService.currentRetrievalSettings()
        );
    }

    private WorkflowContextDiagnosticsView buildContextDiagnostics(String threadId,
                                                                  ContextLoadingDiagnostics contextLoadingDiagnostics) {
        long totalMessageCount = contextLoadingDiagnostics.totalMessageCount();
        Optional<ConversationSummary> latestSummary = conversationSummaryRepository.findLatestByThreadId(threadId);
        boolean persistedSummaryCoversCurrentThread = latestSummary
                .map(summary -> summary.getCoveredMessageCount() >= totalMessageCount)
                .orElse(false);
        return new WorkflowContextDiagnosticsView(
                contextMemoryProperties.enabled(),
                contextMemoryProperties.compressionEnabled(),
                contextMemoryProperties.llmSummaryEnabled(),
                contextMemoryProperties.recentRoundLimit(),
                contextMemoryProperties.summaryThreshold(),
                totalMessageCount,
                contextLoadingDiagnostics.recentMessageCount(),
                contextLoadingDiagnostics.compressionAttempted(),
                contextLoadingDiagnostics.compressionSucceeded(),
                contextLoadingDiagnostics.compressionDecision(),
                contextLoadingDiagnostics.compressionSkipReason(),
                contextLoadingDiagnostics.summaryResolutionSource(),
                contextLoadingDiagnostics.restoredPersistedSummaryToMemory(),
                persistedSummaryCoversCurrentThread,
                latestSummary.map(this::toPersistedSummaryView).orElse(null)
        );
    }

    private WorkflowBusinessFactDiagnosticsView buildBusinessFactDiagnostics(IntentRouteResult routeResult,
                                                                            BusinessFactResult businessFactResult) {
        return new WorkflowBusinessFactDiagnosticsView(
                businessFactResult.status().name(),
                splitSourceSystems(businessFactResult.sourceSystem()),
                requiredFactTypes(routeResult.subIntent()),
                inferFactRole(businessFactResult.status(), routeResult.subIntent()),
                businessFactResult.resolvedEntities(),
                businessFactResult.facts(),
                businessFactResult.missingEntities(),
                businessFactResult.conflictFlags(),
                businessFactResult.factTimestamp()
        );
    }

    private WorkflowPersistedSummaryView toPersistedSummaryView(ConversationSummary summary) {
        return new WorkflowPersistedSummaryView(
                summary.getSummaryId(),
                summary.getSummarySource(),
                summary.getCoveredMessageCount(),
                summary.getCreatedAt(),
                summary.getSummaryText()
        );
    }

    private WorkflowKnowledgeDiagnosticsView buildKnowledgeDiagnostics(RetrievalQuery retrievalQuery,
                                                                      IntentRouteResult routeResult,
                                                                      BusinessFactResult businessFactResult,
                                                                      KnowledgeRetrieveResult knowledgeRetrieveResult) {
        HybridSearchService hybridSearchService = hybridSearchServiceProvider.getIfAvailable();
        if (hybridSearchService == null) {
            return new WorkflowKnowledgeDiagnosticsView(
                    retrievalQuery,
                    retrievalConfigService.currentRetrievalSettings(),
                    knowledgeRetrieveResult.source(),
                    inferFusionStrategy(knowledgeRetrieveResult, false),
                    inferKnowledgeRole(routeResult.subIntent()),
                    factsFirstApplied(retrievalQuery),
                    knowledgeRetrieveResult.recallCount(),
                    knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList(),
                    buildKnowledgePreview(knowledgeRetrieveResult),
                    buildFactGroundingSignals(retrievalQuery, businessFactResult, knowledgeRetrieveResult),
                    false,
                    List.of(),
                    List.of()
            );
        }
        try {
            // 诊断接口额外执行一次检索，用于展示 bm25 / vector 两路召回明细，方便后续调参。
            HybridRetrievalResult hybridRetrievalResult = hybridSearchService.search(retrievalQuery);
            return new WorkflowKnowledgeDiagnosticsView(
                    retrievalQuery,
                    retrievalConfigService.currentRetrievalSettings(),
                    knowledgeRetrieveResult.source(),
                    inferFusionStrategy(knowledgeRetrieveResult, true),
                    inferKnowledgeRole(routeResult.subIntent()),
                    factsFirstApplied(retrievalQuery),
                    knowledgeRetrieveResult.recallCount(),
                    knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList(),
                    buildKnowledgePreview(knowledgeRetrieveResult),
                    buildFactGroundingSignals(retrievalQuery, businessFactResult, knowledgeRetrieveResult),
                    true,
                    hybridRetrievalResult.bm25Snippets(),
                    hybridRetrievalResult.vectorSnippets()
            );
        } catch (RuntimeException ex) {
            return new WorkflowKnowledgeDiagnosticsView(
                    retrievalQuery,
                    retrievalConfigService.currentRetrievalSettings(),
                    knowledgeRetrieveResult.source(),
                    inferFusionStrategy(knowledgeRetrieveResult, false),
                    inferKnowledgeRole(routeResult.subIntent()),
                    factsFirstApplied(retrievalQuery),
                    knowledgeRetrieveResult.recallCount(),
                    knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList(),
                    buildKnowledgePreview(knowledgeRetrieveResult),
                    buildFactGroundingSignals(retrievalQuery, businessFactResult, knowledgeRetrieveResult),
                    false,
                    List.of(),
                    List.of()
            );
        }
    }

    private String inferFusionStrategy(KnowledgeRetrieveResult knowledgeRetrieveResult, boolean hybridDebugAvailable) {
        if (hybridDebugAvailable && "elasticsearch-hybrid".equals(knowledgeRetrieveResult.source())) {
            return "rrf";
        }
        if ("static-knowledge-retriever".equals(knowledgeRetrieveResult.source())) {
            return "score_sort";
        }
        return hybridDebugAvailable ? "hybrid_unknown" : "single_path";
    }

    private List<String> requiredFactTypes(String subIntent) {
        return switch (subIntent) {
            case "logistics_tracking" -> List.of("order", "logistics");
            case "order_status" -> List.of("order");
            case "after_sales_policy", "return_refund" -> List.of("order", "policy");
            default -> List.of();
        };
    }

    private String inferFactRole(BusinessFactStatus factStatus, String subIntent) {
        if (factStatus == BusinessFactStatus.NOT_REQUIRED) {
            return "business facts are not required for this route";
        }
        if (factStatus == BusinessFactStatus.INSUFFICIENT_INPUT) {
            return "business facts are blocked until key identifiers are provided";
        }
        if (factStatus == BusinessFactStatus.CONFLICT) {
            return "business facts act as an authority check and currently conflict";
        }
        if (factStatus == BusinessFactStatus.NO_RESULT) {
            return "business facts were queried but did not return a usable record";
        }
        if (factStatus == BusinessFactStatus.TEMPORARY_FAILURE) {
            return "business facts are required but the upstream query is temporarily unavailable";
        }
        return switch (subIntent) {
            case "logistics_tracking" -> "business facts provide the latest order and logistics truth";
            case "order_status" -> "business facts provide the current order truth";
            case "after_sales_policy", "return_refund" -> "business facts provide order truth before policy guidance is applied";
            default -> "business facts provide authoritative context for this route";
        };
    }

    private String inferKnowledgeRole(String subIntent) {
        return switch (subIntent) {
            case "product_recommendation", "product_comparison", "inventory_or_shipping" ->
                    "knowledge fills product and catalog guidance that business facts do not provide";
            case "after_sales_policy", "return_refund" ->
                    "knowledge supplements policy wording and handling guidance after business facts are checked";
            case "logistics_tracking", "order_status" ->
                    "knowledge supplements explanation and expectation setting around the current business facts";
            default -> "knowledge supplements general response guidance for the current route";
        };
    }

    private boolean factsFirstApplied(RetrievalQuery retrievalQuery) {
        return !retrievalQuery.filters().isEmpty() && retrievalConfigService.currentRetrievalSettings().factsFirst();
    }

    private List<String> buildKnowledgePreview(KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return knowledgeRetrieveResult.snippets().stream()
                .limit(3)
                .map(snippet -> snippet.title() + ": " + preview(snippet.content()))
                .toList();
    }

    private List<String> buildFactGroundingSignals(RetrievalQuery retrievalQuery,
                                                   BusinessFactResult businessFactResult,
                                                   KnowledgeRetrieveResult knowledgeRetrieveResult) {
        List<String> matched = new java.util.ArrayList<>();
        for (String filter : retrievalQuery.filters()) {
            String normalizedFilter = normalizeSignal(filter);
            if (normalizedFilter.isBlank()) {
                continue;
            }
            boolean foundInSnippet = knowledgeRetrieveResult.snippets().stream().anyMatch(snippet ->
                    normalizeSignal(snippet.title()).contains(normalizedFilter)
                            || normalizeSignal(snippet.content()).contains(normalizedFilter));
            if (foundInSnippet) {
                matched.add("filter_grounded=" + filter);
            }
        }
        for (String fact : businessFactResult.facts()) {
            String normalizedFact = normalizeSignal(fact);
            if (normalizedFact.isBlank()) {
                continue;
            }
            boolean foundInSnippet = knowledgeRetrieveResult.snippets().stream().anyMatch(snippet ->
                    normalizeSignal(snippet.content()).contains(normalizedFact));
            if (foundInSnippet) {
                matched.add("fact_grounded=" + fact);
            }
        }
        return matched.stream().distinct().toList();
    }

    private List<String> splitSourceSystems(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(sourceSystem.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }

    private String normalizeSignal(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace("=", " ")
                .replace("_", " ")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private WorkflowReplyDiagnosticsView buildReplyDiagnostics(ReplyDraftingDiagnostics diagnostics) {
        return new WorkflowReplyDiagnosticsView(
                diagnostics.draftStatus(),
                diagnostics.replySource(),
                diagnostics.llmAttempted(),
                diagnostics.llmResponseAccepted(),
                diagnostics.fallbackReason(),
                diagnostics.systemPrompt(),
                diagnostics.userPrompt(),
                diagnostics.factPreview(),
                diagnostics.knowledgeSnippetIds(),
                diagnostics.knowledgeSnippetPreview()
        );
    }

    private List<String> mergeActions(List<String> primaryActions, List<String> secondaryActions) {
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        merged.addAll(primaryActions);
        merged.addAll(secondaryActions);
        return List.copyOf(merged);
    }
}
