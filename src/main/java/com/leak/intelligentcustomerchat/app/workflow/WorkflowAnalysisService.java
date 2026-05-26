package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.config.IntentConfigService;
import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingDiagnostics;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingTraceService;
import com.leak.intelligentcustomerchat.app.intent.IntentHeuristicPreviewService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationDiagnostics;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationTraceService;
import com.leak.intelligentcustomerchat.app.intent.IntentRoutingService;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrieveService;
import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionContext;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionService;
import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
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
    private final BusinessFactService businessFactService;
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
                                   BusinessFactService businessFactService,
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
        this.businessFactService = businessFactService;
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
        BusinessFactResult businessFactResult = businessFactService.loadFacts(cleanedMail, normalizationResult, routeResult, contextSnapshot);
        RetrievalQuery retrievalQuery = buildRetrievalQuery(normalizationResult, routeResult, businessFactResult);
        KnowledgeRetrieveResult knowledgeRetrieveResult = knowledgeRetrieveService.retrieve(normalizationResult, routeResult, businessFactResult);

        // 分析视图不落正式状态机，只复用同一套草稿与审核能力，方便快速检查中间产物。
        WorkflowRun previewRun = WorkflowRun.start(cleanedMail.messageId(), cleanedMail.threadId());
        ReplyDraft draft = replyDraftService.draft(
                previewRun,
                cleanedMail,
                normalizationResult,
                routeResult,
                contextSnapshot,
                businessFactResult,
                knowledgeRetrieveResult
        );
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
                        normalizationDiagnostics.guardrailActions()
                ),
                normalizationResult,
                routeResult,
                contextSnapshot,
                buildContextDiagnostics(cleanedMail.threadId(), contextLoadingDiagnostics),
                businessFactResult,
                buildKnowledgeDiagnostics(retrievalQuery, knowledgeRetrieveResult),
                knowledgeRetrieveResult,
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
                                               BusinessFactResult businessFactResult) {
        return new RetrievalQuery(
                normalizationResult.normalizedRequest(),
                routeResult.scene().name(),
                routeResult.subIntent(),
                businessFactResult.resolvedEntities(),
                retrievalConfigService.currentRetrievalSettings().topK()
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
                                                                      KnowledgeRetrieveResult knowledgeRetrieveResult) {
        HybridSearchService hybridSearchService = hybridSearchServiceProvider.getIfAvailable();
        if (hybridSearchService == null) {
            return new WorkflowKnowledgeDiagnosticsView(
                    retrievalQuery,
                    retrievalConfigService.currentRetrievalSettings(),
                    knowledgeRetrieveResult.source(),
                    inferFusionStrategy(knowledgeRetrieveResult, false),
                    knowledgeRetrieveResult.recallCount(),
                    knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList(),
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
                    knowledgeRetrieveResult.recallCount(),
                    knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList(),
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
                    knowledgeRetrieveResult.recallCount(),
                    knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList(),
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
}
