package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.app.ai.LlmClient;
import com.leak.intelligentcustomerchat.app.config.PromptConfigService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.runtime.PromptSceneTemplateConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateReplyDraftServiceTest {

    @Test
    void shouldUseFollowUpTemplateWhenInputIsIncomplete() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.of("should-not-be-used")));

        ReplyDraftingResult result = service.draftWithDiagnostics(
                WorkflowRun.start("msg-1", "thread-1"),
                mail(),
                normalizationResult(ProcessingDisposition.FOLLOW_UP),
                routeResult(),
                new ContextSnapshot("existing-summary", List.of(), List.of()),
                BusinessFactResult.insufficientInput(List.of("order_id_or_tracking_no")),
                KnowledgeRetrieveResult.empty()
        );
        ReplyDraft draft = result.draft();

        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.FOLLOW_UP_NEEDED);
        assertThat(draft.getBody().toLowerCase()).contains("please share your order number");
        assertThat(draft.getReviewNotes()).contains("replySource=follow-up-template");
        assertThat(result.diagnostics().replySource()).isEqualTo("follow-up-template");
        assertThat(result.diagnostics().llmAttempted()).isFalse();
        assertThat(result.diagnostics().fallbackReason()).isEqualTo("follow_up_template_required");
        assertThat(result.diagnostics().coverageMode()).isEqualTo("primary_only");
    }

    @Test
    void shouldUseLlmReplyWhenDirectDraftIsReady() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.of("""
                Hello,

                We checked the latest details and your order is currently in transit.
                We will keep monitoring the shipment and update you if anything changes.
                """)));

        ReplyDraftingResult result = service.draftWithDiagnostics(
                WorkflowRun.start("msg-1", "thread-1"),
                mail(),
                normalizationResult(ProcessingDisposition.CONTINUE),
                routeResult(),
                new ContextSnapshot("customer asked for shipping progress yesterday", List.of(), List.of()),
                successFacts(),
                knowledgeResult()
        );
        ReplyDraft draft = result.draft();

        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(draft.getBody()).contains("currently in transit");
        assertThat(draft.getReviewNotes()).contains("replySource=llm");
        assertThat(result.diagnostics().replySource()).isEqualTo("llm");
        assertThat(result.diagnostics().llmAttempted()).isTrue();
        assertThat(result.diagnostics().llmResponseAccepted()).isTrue();
        assertThat(result.diagnostics().contextMode()).isEqualTo("summary_only");
        assertThat(result.diagnostics().coverageMode()).isEqualTo("primary_only");
        assertThat(result.diagnostics().coveredQuestions()).containsExactly("What is the shipment status?");
        assertThat(result.diagnostics().deferredQuestions()).isEmpty();
        assertThat(result.diagnostics().systemPrompt()).contains("safe and concise customer reply");
        assertThat(result.diagnostics().userPrompt()).contains("Customer email subject");
        assertThat(result.diagnostics().userPrompt()).contains("Context mode:");
        assertThat(result.diagnostics().factPreview()).contains("order confirmed");
        assertThat(result.diagnostics().knowledgeSnippetIds()).containsExactly("k-1");
    }

    @Test
    void shouldFallbackToTemplateReplyWhenLlmIsUnavailable() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.empty()));

        ReplyDraftingResult result = service.draftWithDiagnostics(
                WorkflowRun.start("msg-1", "thread-1"),
                mail(),
                normalizationResult(ProcessingDisposition.CONTINUE),
                routeResult(),
                new ContextSnapshot("customer asked for shipping progress yesterday", List.of(), List.of()),
                successFacts(),
                knowledgeResult()
        );
        ReplyDraft draft = result.draft();

        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(draft.getBody()).contains("Current update:");
        assertThat(draft.getBody()).contains("shipping-related information");
        assertThat(draft.getBody()).contains("mode: summary_only");
        assertThat(draft.getReviewNotes()).contains("replySource=template");
        assertThat(result.diagnostics().replySource()).isEqualTo("template");
        assertThat(result.diagnostics().llmAttempted()).isTrue();
        assertThat(result.diagnostics().llmResponseAccepted()).isFalse();
        assertThat(result.diagnostics().fallbackReason()).isEqualTo("llm_unavailable_or_empty");
    }

    @Test
    void shouldPreferRecentMessagesWhenOnlySyntheticSummaryExists() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.of("ok")));

        ReplyDraftingResult result = service.draftWithDiagnostics(
                WorkflowRun.start("msg-1", "thread-1"),
                mail(),
                normalizationResult(ProcessingDisposition.CONTINUE),
                routeResult(),
                new ContextSnapshot(
                        "thread=thread-1, latestSubject=Shipping update, route=AFTER_SALES",
                        List.of("tracking_number=ZX987654"),
                        List.of("Earlier the customer asked about the same shipment progress.")
                ),
                successFacts(),
                knowledgeResult()
        );

        assertThat(result.diagnostics().contextMode()).isEqualTo("recent_messages_only");
        assertThat(result.diagnostics().contextPreview()).contains("recent=Earlier the customer asked about the same shipment progress.");
        assertThat(result.diagnostics().contextStrongSignals()).containsExactly("tracking_number=ZX987654");
        assertThat(result.diagnostics().userPrompt()).contains("Recent conversation rounds:");
        assertThat(result.diagnostics().userPrompt()).contains("Earlier the customer asked about the same shipment progress.");
    }

    @Test
    void shouldDeferSecondaryQuestionsInsteadOfMixingThemIntoSingleTemplateReply() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.empty()));

        ReplyDraftingResult result = service.draftWithDiagnostics(
                WorkflowRun.start("msg-3", "thread-3"),
                mail(),
                normalizationResult(ProcessingDisposition.CONTINUE, List.of("Can you also confirm the estimated delivery time?")),
                routeResult(),
                new ContextSnapshot("customer asked for shipping progress yesterday", List.of(), List.of()),
                successFacts(),
                knowledgeResult()
        );

        assertThat(result.diagnostics().coverageMode()).isEqualTo("primary_with_deferred_secondary");
        assertThat(result.diagnostics().coveredQuestions()).containsExactly("What is the shipment status?");
        assertThat(result.diagnostics().deferredQuestions()).containsExactly("Can you also confirm the estimated delivery time?");
        assertThat(result.diagnostics().userPrompt()).contains("Deferred question(s):");
        assertThat(result.draft().getBody()).contains("Scope note:");
        assertThat(result.draft().getBody()).contains("Additional questions captured for follow-up");
    }

    @Test
    void shouldUseIntentAwareTemplateForPreSalesRecommendationWhenLlmIsUnavailable() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.empty()));

        ReplyDraft draft = service.draft(
                WorkflowRun.start("msg-2", "thread-2"),
                new InboundMail(
                        "msg-2",
                        "thread-2",
                        "customer@example.com",
                        "Need recommendation",
                        "Please recommend a product for a living room.",
                        OffsetDateTime.now()
                ),
                new IntentNormalizationResult(
                        "Customer wants a product recommendation for the living room.",
                        "What product would fit the living room?",
                        List.of(),
                        List.of(CustomerScene.PRE_SALES),
                        List.of("product_recommendation"),
                        List.of(),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.PRE_SALES, "product_recommendation", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("customer asked for a living room suggestion", List.of(), List.of()),
                BusinessFactResult.notRequired(),
                new KnowledgeRetrieveResult(
                        "knowledge-index",
                        List.of(new KnowledgeSnippet("k-2", "recommendation", "Warm ambient options are usually suitable for living room relaxation.", 0.93d, "faq")),
                        1
                )
        );

        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(draft.getBody()).contains("first recommendation direction");
        assertThat(draft.getBody()).contains("living room");
    }

    @Test
    void shouldUseSceneSpecificFollowUpTemplateForPreSalesRequests() {
        TemplateReplyDraftService service = new TemplateReplyDraftService(promptConfigService(), new StubLlmClient(Optional.empty()));

        ReplyDraft draft = service.draft(
                WorkflowRun.start("msg-4", "thread-4"),
                new InboundMail(
                        "msg-4",
                        "thread-4",
                        "customer@example.com",
                        "Need advice",
                        "I need help choosing a product but I have not shared enough detail yet.",
                        OffsetDateTime.now()
                ),
                new IntentNormalizationResult(
                        "Customer needs product advice but has not shared enough preference detail.",
                        "What product would fit my room setup?",
                        List.of(),
                        List.of(CustomerScene.PRE_SALES),
                        List.of("product_recommendation"),
                        List.of("room_context"),
                        List.of("room_context"),
                        ProcessingDisposition.FOLLOW_UP
                ),
                new IntentRouteResult(CustomerScene.PRE_SALES, "product_recommendation", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("customer asks for a recommendation", List.of(), List.of()),
                BusinessFactResult.notRequired(),
                KnowledgeRetrieveResult.empty()
        );

        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.FOLLOW_UP_NEEDED);
        assertThat(draft.getBody()).contains("preferred style");
        assertThat(draft.getBody()).doesNotContain("order number or tracking number");
    }

    private static PromptConfigService promptConfigService() {
        PromptTemplateConfig config = new PromptTemplateConfig(
                "Return JSON only.",
                "Write a safe and concise customer reply.",
                """
                Hello,

                Please share your order number or tracking number.
                Main request: {{primaryQuestion}}
                Scene: {{scene}}
                """,
                """
                Hello,

                A specialist is reviewing your request.
                Main request: {{primaryQuestion}}
                Scene: {{scene}}
                """,
                "Close politely and avoid unsupported promises.",
                new PromptSceneTemplateConfig(
                        java.util.Map.of(
                                "PRE_SALES", """
                                Hello,

                                Please share a little more detail about your intended use, preferred style, or room setup.
                                Main request: {{primaryQuestion}}
                                Scene: {{scene}}
                                """,
                                "AFTER_SALES", """
                                Hello,

                                Please share your order number or tracking number.
                                Main request: {{primaryQuestion}}
                                Scene: {{scene}}
                                """
                        ),
                        java.util.Map.of(
                                "PRE_SALES", """
                                Hello,

                                A specialist is reviewing your recommendation request.
                                Main request: {{primaryQuestion}}
                                Scene: {{scene}}
                                """,
                                "AFTER_SALES", """
                                Hello,

                                A specialist is reviewing your order-related request.
                                Main request: {{primaryQuestion}}
                                Scene: {{scene}}
                                """
                        ),
                        java.util.Map.of(
                                "PRE_SALES", "Close politely and mention that richer recommendation evidence will be added later.",
                                "AFTER_SALES", "Close politely and avoid unsupported promises."
                        )
                )
        );
        return () -> config;
    }

    private static InboundMail mail() {
        return new InboundMail(
                "msg-1",
                "thread-1",
                "customer@example.com",
                "Shipping update",
                "Can you check the status of my shipment?",
                OffsetDateTime.now()
        );
    }

    private static IntentNormalizationResult normalizationResult(ProcessingDisposition disposition) {
        return normalizationResult(disposition, List.of());
    }

    private static IntentNormalizationResult normalizationResult(ProcessingDisposition disposition, List<String> secondaryQuestions) {
        return new IntentNormalizationResult(
                secondaryQuestions.isEmpty()
                        ? "Customer asks for shipping status."
                        : "Customer asks for shipping status. Secondary questions: " + String.join("; ", secondaryQuestions),
                "What is the shipment status?",
                secondaryQuestions,
                List.of(CustomerScene.AFTER_SALES),
                List.of("logistics_tracking"),
                List.of("order_id_or_tracking_no"),
                disposition == ProcessingDisposition.FOLLOW_UP ? List.of("order_id_or_tracking_no") : List.of(),
                disposition
        );
    }

    private static IntentRouteResult routeResult() {
        return new IntentRouteResult(CustomerScene.AFTER_SALES, "logistics_tracking", ProcessingDisposition.CONTINUE, "test");
    }

    private static BusinessFactResult successFacts() {
        return new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                "oms",
                List.of("order_id:AB12345678"),
                List.of("order confirmed", "shipment in transit"),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }

    private static KnowledgeRetrieveResult knowledgeResult() {
        return new KnowledgeRetrieveResult(
                "knowledge-index",
                List.of(new KnowledgeSnippet("k-1", "shipping faq", "Transit updates may lag by 12-24 hours.", 0.91d, "faq")),
                1
        );
    }

    private record StubLlmClient(Optional<String> response) implements LlmClient {
        @Override
        public Optional<String> complete(String systemPrompt, String userPrompt) {
            return response;
        }
    }
}
