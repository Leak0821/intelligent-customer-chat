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
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftingDiagnostics;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftingResult;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionService;
import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummaryRepository;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.HybridRetrievalResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.HybridSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAnalysisServiceTest {

    @Test
    void shouldAssembleIntermediateArtifactsForDemoAnalysis() {
        InboundMail inboundMail = new InboundMail(
                "msg-1",
                "thread-1",
                "customer@example.com",
                "Need help",
                "Please check tracking number ZX987654",
                OffsetDateTime.now()
        );
        InboundMail cleanedMail = inboundMail.withRawBody("Please check tracking number ZX987654");
        IntentNormalizationResult normalizationResult = new IntentNormalizationResult(
                "Customer wants the tracking status.",
                "What is the tracking status?",
                List.of(),
                List.of(CustomerScene.AFTER_SALES),
                List.of("logistics_tracking"),
                List.of("order_id_or_tracking_no"),
                List.of(),
                ProcessingDisposition.CONTINUE
        );
        IntentRouteResult routeResult = new IntentRouteResult(CustomerScene.AFTER_SALES, "logistics_tracking", ProcessingDisposition.CONTINUE, "stub");
        ContextSnapshot contextSnapshot = new ContextSnapshot("customer asked again after previous email", List.of("tracking_number=ZX987654"), List.of());
        BusinessFactResult businessFactResult = new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                "stub-gateway",
                List.of("tracking_number=ZX987654"),
                List.of("current logistics status=in_transit"),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
        KnowledgeRetrieveResult knowledgeRetrieveResult = new KnowledgeRetrieveResult(
                "elasticsearch-hybrid",
                List.of(new KnowledgeSnippet("fused-1", "fused title", "fused content", 1.1d, "rrf")),
                1
        );
        ReplyDraft draft = ReplyDraft.create("preview-run", "Re: Need help", "Draft body", ReplyDraftStatus.DRAFT_READY, "notes");
        ReviewDecision reviewDecision = new ReviewDecision(ReplyDraftStatus.DRAFT_READY, false, "ready");
        ConversationSummary persistedSummary = ConversationSummary.create(
                "thread-1",
                "customer asked again after previous email",
                "heuristic",
                4
        );
        ConversationMemoryStore conversationMemoryStore = new StubConversationMemoryStore(List.of("older mail", "latest mail"), 4L);
        ConversationSummaryRepository conversationSummaryRepository = new StubConversationSummaryRepository(persistedSummary);
        IntentConfigService intentConfigService = () -> new IntentCatalogConfig(
                List.of("product_recommendation", "inventory_or_shipping"),
                List.of("logistics_tracking", "after_sales_policy")
        );
        RetrievalConfigService retrievalConfigService = () -> new RetrievalSettingsConfig(10, true, 60);
        HybridSearchService hybridSearchService = query -> new HybridRetrievalResult(
                List.of(),
                List.of(new KnowledgeSnippet("bm25-1", "bm25 title", "bm25 content", 1.0d, "bm25")),
                List.of(new KnowledgeSnippet("vector-1", "vector title", "vector content", 0.9d, "vector"))
        );

        WorkflowAnalysisService service = new WorkflowAnalysisService(
                mail -> cleanedMail,
                new IntentHeuristicPreviewService(),
                mail -> normalizationResult,
                new SingleObjectProvider<IntentNormalizationTraceService>(mail -> new IntentNormalizationDiagnostics(
                        normalizationResult,
                        normalizationResult,
                        "llm_with_guardrails",
                        true,
                        true,
                        null,
                        List.of("enforce_order_id_for_after_sales"),
                        List.of("scene_after_sales", "sub_intent_logistics_tracking")
                )),
                new SingleObjectProvider<ContextLoadingTraceService>((mail, route) -> new ContextLoadingDiagnostics(
                        contextSnapshot,
                        4L,
                        2,
                        true,
                        true,
                        "generated",
                        null,
                        "memory_summary",
                        false
                )),
                normalization -> routeResult,
                (mail, route) -> contextSnapshot,
                (mail, normalization, route, context) -> businessFactResult,
                (normalization, route, facts) -> knowledgeRetrieveResult,
                new ReplyDraftService() {
                    @Override
                    public ReplyDraft draft(WorkflowRun run,
                                            InboundMail mail,
                                            IntentNormalizationResult normalization,
                                            IntentRouteResult route,
                                            ContextSnapshot context,
                                            BusinessFactResult facts,
                                            KnowledgeRetrieveResult knowledge) {
                        assertThat(run.getMessageId()).isEqualTo("msg-1");
                        assertThat(run.getThreadId()).isEqualTo("thread-1");
                        return draft;
                    }

                    @Override
                    public ReplyDraftingResult draftWithDiagnostics(WorkflowRun run,
                                                                    InboundMail mail,
                                                                    IntentNormalizationResult normalization,
                                                                    IntentRouteResult route,
                                                                    ContextSnapshot context,
                                                                    BusinessFactResult facts,
                                                                    KnowledgeRetrieveResult knowledge) {
                        return new ReplyDraftingResult(
                                draft(run, mail, normalization, route, context, facts, knowledge),
                                new ReplyDraftingDiagnostics(
                                        ReplyDraftStatus.DRAFT_READY,
                                        "llm",
                                        true,
                                        true,
                                        null,
                                        "Write a safe and concise customer reply.",
                                        "Customer email subject:\nNeed help",
                                        List.of("current logistics status=in_transit"),
                                        List.of("fused-1"),
                                        List.of("fused content")
                                )
                        );
                    }
                },
                (replyDraft, context) -> {
                    assertThat(context.routeResult()).isEqualTo(routeResult);
                    assertThat(context.businessFactResult()).isEqualTo(businessFactResult);
                    assertThat(context.knowledgeRetrieveResult()).isEqualTo(knowledgeRetrieveResult);
                    return reviewDecision;
                },
                intentConfigService,
                retrievalConfigService,
                conversationMemoryStore,
                conversationSummaryRepository,
                new ContextMemoryProperties(true, true, false, 5, 3, "ctx"),
                new SingleObjectProvider<>(hybridSearchService)
        );

        WorkflowAnalysisView view = service.analyze(inboundMail);

        assertThat(view.cleanedMail()).isEqualTo(cleanedMail);
        assertThat(view.normalizationResult()).isEqualTo(normalizationResult);
        assertThat(view.routeResult()).isEqualTo(routeResult);
        assertThat(view.contextSnapshot()).isEqualTo(contextSnapshot);
        assertThat(view.businessFactResult()).isEqualTo(businessFactResult);
        assertThat(view.knowledgeRetrieveResult()).isEqualTo(knowledgeRetrieveResult);
        assertThat(view.draft()).isEqualTo(draft);
        assertThat(view.reviewDecision()).isEqualTo(reviewDecision);
        assertThat(view.intentDiagnostics().normalizationSource()).isEqualTo("llm_with_guardrails");
        assertThat(view.intentDiagnostics().llmAttempted()).isTrue();
        assertThat(view.intentDiagnostics().llmResponseAccepted()).isTrue();
        assertThat(view.intentDiagnostics().guardrailActions()).contains("enforce_order_id_for_after_sales");
        assertThat(view.intentDiagnostics().heuristicMatchedSignals()).contains("scene_after_sales", "sub_intent_logistics_tracking");
        assertThat(view.intentDiagnostics().intentCatalog().afterSalesIntents()).contains("logistics_tracking");
        assertThat(view.intentDiagnostics().heuristicBaseline().sceneCandidates()).contains(CustomerScene.AFTER_SALES);
        assertThat(view.contextDiagnostics().totalMessageCount()).isEqualTo(4L);
        assertThat(view.contextDiagnostics().compressionAttempted()).isTrue();
        assertThat(view.contextDiagnostics().compressionSucceeded()).isTrue();
        assertThat(view.contextDiagnostics().compressionDecision()).isEqualTo("generated");
        assertThat(view.contextDiagnostics().summaryResolutionSource()).isEqualTo("memory_summary");
        assertThat(view.contextDiagnostics().persistedSummaryCoversCurrentThread()).isTrue();
        assertThat(view.contextDiagnostics().latestPersistedSummary()).isNotNull();
        assertThat(view.businessFactDiagnostics().factStatus()).isEqualTo("SUCCESS");
        assertThat(view.businessFactDiagnostics().sourceSystems()).containsExactly("stub-gateway");
        assertThat(view.businessFactDiagnostics().requiredFactTypes()).containsExactly("order", "logistics");
        assertThat(view.businessFactDiagnostics().factRole()).contains("latest order and logistics truth");
        assertThat(view.businessFactDiagnostics().resolvedEntities()).contains("tracking_number=ZX987654");
        assertThat(view.knowledgeDiagnostics().retrievalQuery().subIntent()).isEqualTo("logistics_tracking");
        assertThat(view.knowledgeDiagnostics().retrievalSource()).isEqualTo("elasticsearch-hybrid");
        assertThat(view.knowledgeDiagnostics().fusionStrategy()).isEqualTo("rrf");
        assertThat(view.knowledgeDiagnostics().knowledgeRole()).contains("expectation setting");
        assertThat(view.knowledgeDiagnostics().factsFirstApplied()).isTrue();
        assertThat(view.knowledgeDiagnostics().recallCount()).isEqualTo(1);
        assertThat(view.knowledgeDiagnostics().fusedSnippetIds()).containsExactly("fused-1");
        assertThat(view.knowledgeDiagnostics().knowledgePreview()).hasSize(1);
        assertThat(view.knowledgeDiagnostics().factGroundingSignals()).isEmpty();
        assertThat(view.knowledgeDiagnostics().hybridDebugAvailable()).isTrue();
        assertThat(view.knowledgeDiagnostics().bm25Snippets()).hasSize(1);
        assertThat(view.knowledgeDiagnostics().vectorSnippets()).hasSize(1);
        assertThat(view.replyDiagnostics().replySource()).isEqualTo("llm");
        assertThat(view.replyDiagnostics().llmAttempted()).isTrue();
        assertThat(view.replyDiagnostics().llmResponseAccepted()).isTrue();
        assertThat(view.replyDiagnostics().systemPrompt()).contains("safe and concise customer reply");
        assertThat(view.replyDiagnostics().factPreview()).contains("current logistics status=in_transit");
        assertThat(view.replyDiagnostics().knowledgeSnippetIds()).containsExactly("fused-1");
    }

    private static final class StubConversationMemoryStore implements ConversationMemoryStore {
        private final List<String> recentMessages;
        private final long totalMessageCount;

        private StubConversationMemoryStore(List<String> recentMessages, long totalMessageCount) {
            this.recentMessages = recentMessages;
            this.totalMessageCount = totalMessageCount;
        }

        @Override
        public ContextSnapshot read(String threadId) {
            return ContextSnapshot.empty();
        }

        @Override
        public void appendCustomerMessage(String threadId, String message) {
        }

        @Override
        public List<String> recentMessages(String threadId) {
            return recentMessages;
        }

        @Override
        public void saveSummary(String threadId, String summary) {
        }

        @Override
        public long totalMessageCount(String threadId) {
            return totalMessageCount;
        }
    }

    private static final class StubConversationSummaryRepository implements ConversationSummaryRepository {
        private final ConversationSummary summary;

        private StubConversationSummaryRepository(ConversationSummary summary) {
            this.summary = summary;
        }

        @Override
        public ConversationSummary save(ConversationSummary summary) {
            return summary;
        }

        @Override
        public Optional<ConversationSummary> findLatestByThreadId(String threadId) {
            return Optional.of(summary);
        }
    }

    private static final class SingleObjectProvider<T> implements ObjectProvider<T> {
        private final T value;

        private SingleObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}
