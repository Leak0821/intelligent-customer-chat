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
        assertThat(result.diagnostics().systemPrompt()).contains("safe and concise customer reply");
        assertThat(result.diagnostics().userPrompt()).contains("Customer email subject");
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
        assertThat(draft.getReviewNotes()).contains("replySource=template");
        assertThat(result.diagnostics().replySource()).isEqualTo("template");
        assertThat(result.diagnostics().llmAttempted()).isTrue();
        assertThat(result.diagnostics().llmResponseAccepted()).isFalse();
        assertThat(result.diagnostics().fallbackReason()).isEqualTo("llm_unavailable_or_empty");
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
                "Close politely and avoid unsupported promises."
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
        return new IntentNormalizationResult(
                "Customer asks for shipping status.",
                "What is the shipment status?",
                List.of(),
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
