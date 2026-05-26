package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.config.IntentConfigService;
import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.intent.IntentHeuristicPreviewService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
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
        IntentNormalizationResult heuristicBaseline = intentHeuristicPreviewService.preview(cleanedMail);
        IntentNormalizationResult normalizationResult = intentNormalizationService.normalize(cleanedMail);
        IntentRouteResult routeResult = intentRoutingService.route(normalizationResult);
        ContextSnapshot contextSnapshot = contextLoadingService.load(cleanedMail, routeResult);
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
                        !heuristicBaseline.equals(normalizationResult)
                ),
                normalizationResult,
                routeResult,
                contextSnapshot,
                buildContextDiagnostics(cleanedMail.threadId()),
                businessFactResult,
                buildKnowledgeDiagnostics(retrievalQuery),
                knowledgeRetrieveResult,
                draft,
                reviewDecision
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

    private WorkflowContextDiagnosticsView buildContextDiagnostics(String threadId) {
        long totalMessageCount = conversationMemoryStore.totalMessageCount(threadId);
        List<String> recentMessages = conversationMemoryStore.recentMessages(threadId);
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
                recentMessages.size(),
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

    private WorkflowKnowledgeDiagnosticsView buildKnowledgeDiagnostics(RetrievalQuery retrievalQuery) {
        HybridSearchService hybridSearchService = hybridSearchServiceProvider.getIfAvailable();
        if (hybridSearchService == null) {
            return new WorkflowKnowledgeDiagnosticsView(
                    retrievalQuery,
                    retrievalConfigService.currentRetrievalSettings(),
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
                    true,
                    hybridRetrievalResult.bm25Snippets(),
                    hybridRetrievalResult.vectorSnippets()
            );
        } catch (RuntimeException ex) {
            return new WorkflowKnowledgeDiagnosticsView(
                    retrievalQuery,
                    retrievalConfigService.currentRetrievalSettings(),
                    false,
                    List.of(),
                    List.of()
            );
        }
    }
}
