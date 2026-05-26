package com.leak.intelligentcustomerchat.app.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.app.ai.LlmClient;
import com.leak.intelligentcustomerchat.app.config.PromptConfigService;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultIntentNormalizationServiceTest {

    @Test
    void shouldFallbackToHeuristicsWhenLlmIsUnavailable() {
        DefaultIntentNormalizationService service = new DefaultIntentNormalizationService(
                promptConfigService(),
                new StubLlmClient(Optional.empty()),
                new ObjectMapper()
        );
        IntentNormalizationDiagnostics diagnostics = service.diagnose(mail(
                "Need tracking help",
                "Where is my order? I still have not received it."
        ));

        IntentNormalizationResult result = diagnostics.finalResult();

        assertThat(result.sceneCandidates()).containsExactly(CustomerScene.AFTER_SALES);
        assertThat(result.subIntentCandidates()).contains("logistics_tracking");
        assertThat(result.missingEntities()).contains("order_id_or_tracking_no");
        assertThat(result.disposition()).isEqualTo(ProcessingDisposition.FOLLOW_UP);
        assertThat(diagnostics.normalizationSource()).isEqualTo("heuristic_fallback");
        assertThat(diagnostics.llmAttempted()).isFalse();
        assertThat(diagnostics.llmResponseAccepted()).isFalse();
        assertThat(diagnostics.fallbackReason()).isEqualTo("llm_unavailable");
    }

    @Test
    void shouldUseStructuredLlmNormalizationWhenResponseIsValid() {
        String json = """
                {
                  "normalizedRequest": "Customer wants a product recommendation for bedroom lighting.",
                  "primaryQuestion": "Which product is suitable for a bedroom setup?",
                  "secondaryQuestions": ["Is warm light available?"],
                  "sceneCandidates": ["PRE_SALES"],
                  "subIntentCandidates": ["product_recommendation"],
                  "requiredEntities": [],
                  "missingEntities": [],
                  "disposition": "CONTINUE"
                }
                """;
        DefaultIntentNormalizationService service = new DefaultIntentNormalizationService(
                promptConfigService(),
                new StubLlmClient(Optional.of(json)),
                new ObjectMapper()
        );

        IntentNormalizationResult result = service.normalize(mail(
                "Need product advice",
                "Please recommend a product for my bedroom."
        ));

        assertThat(result.primaryQuestion()).isEqualTo("Which product is suitable for a bedroom setup?");
        assertThat(result.secondaryQuestions()).containsExactly("Is warm light available?");
        assertThat(result.sceneCandidates()).containsExactly(CustomerScene.PRE_SALES);
        assertThat(result.subIntentCandidates()).containsExactly("product_recommendation");
        assertThat(result.disposition()).isEqualTo(ProcessingDisposition.CONTINUE);
    }

    @Test
    void shouldKeepOrderIdGuardrailEvenWhenLlmAttemptsToRelaxIt() {
        String json = """
                {
                  "normalizedRequest": "Customer asks for tracking progress.",
                  "primaryQuestion": "Where is my shipment now?",
                  "secondaryQuestions": [],
                  "sceneCandidates": ["AFTER_SALES"],
                  "subIntentCandidates": ["logistics_tracking"],
                  "requiredEntities": [],
                  "missingEntities": [],
                  "disposition": "CONTINUE"
                }
                """;
        DefaultIntentNormalizationService service = new DefaultIntentNormalizationService(
                promptConfigService(),
                new StubLlmClient(Optional.of(json)),
                new ObjectMapper()
        );

        IntentNormalizationDiagnostics diagnostics = service.diagnose(mail(
                "Tracking question",
                "Can you tell me where my order is now?"
        ));
        IntentNormalizationResult result = diagnostics.finalResult();

        assertThat(result.requiredEntities()).contains("order_id_or_tracking_no");
        assertThat(result.missingEntities()).contains("order_id_or_tracking_no");
        assertThat(result.disposition()).isEqualTo(ProcessingDisposition.FOLLOW_UP);
        assertThat(diagnostics.guardrailActions()).contains("enforce_order_id_for_after_sales");
        assertThat(diagnostics.normalizationSource()).isEqualTo("llm_with_guardrails");
    }

    @Test
    void shouldFallbackToHeuristicsWhenLlmResponseIsNotValidJson() {
        DefaultIntentNormalizationService service = new DefaultIntentNormalizationService(
                promptConfigService(),
                new StubLlmClient(Optional.of("not-json")),
                new ObjectMapper()
        );

        IntentNormalizationDiagnostics diagnostics = service.diagnose(mail(
                "Tracking question",
                "Can you tell me where my order is now?"
        ));

        assertThat(diagnostics.finalResult()).isEqualTo(diagnostics.heuristicBaseline());
        assertThat(diagnostics.normalizationSource()).isEqualTo("heuristic_fallback");
        assertThat(diagnostics.llmAttempted()).isTrue();
        assertThat(diagnostics.llmResponseAccepted()).isFalse();
        assertThat(diagnostics.fallbackReason()).isEqualTo("llm_response_invalid");
    }

    @Test
    void shouldAllowAfterSalesRequestToContinueWhenExplicitOrderNumberExists() {
        DefaultIntentNormalizationService service = new DefaultIntentNormalizationService(
                promptConfigService(),
                new StubLlmClient(Optional.empty()),
                new ObjectMapper()
        );

        IntentNormalizationResult result = service.normalize(mail(
                "Tracking question",
                "My order number is AB12345678 and I want to know the latest shipment status."
        ));

        assertThat(result.requiredEntities()).contains("order_id_or_tracking_no");
        assertThat(result.missingEntities()).isEmpty();
        assertThat(result.disposition()).isEqualTo(ProcessingDisposition.CONTINUE);
    }

    @Test
    void shouldRecognizeOrderStatusIntentWhenCustomerAsksForShipmentProgressWithoutTrackingId() {
        DefaultIntentNormalizationService service = new DefaultIntentNormalizationService(
                promptConfigService(),
                new StubLlmClient(Optional.empty()),
                new ObjectMapper()
        );

        IntentNormalizationResult result = service.normalize(mail(
                "Order status question",
                "My order number is EFGH5678. Could you let me know the current order status and when it will ship?"
        ));

        assertThat(result.sceneCandidates()).containsExactly(CustomerScene.AFTER_SALES);
        assertThat(result.subIntentCandidates()).contains("order_status");
        assertThat(result.requiredEntities()).contains("order_id_or_tracking_no");
        assertThat(result.missingEntities()).isEmpty();
        assertThat(result.disposition()).isEqualTo(ProcessingDisposition.CONTINUE);
    }

    private static PromptConfigService promptConfigService() {
        PromptTemplateConfig config = new PromptTemplateConfig(
                "Return JSON only.",
                "Write a reply draft.",
                "follow-up",
                "human-review",
                "direct-reply"
        );
        return () -> config;
    }

    private static InboundMail mail(String subject, String body) {
        return new InboundMail("msg-1", "thread-1", "customer@example.com", subject, body, OffsetDateTime.now());
    }

    private record StubLlmClient(Optional<String> response) implements LlmClient {
        @Override
        public Optional<String> complete(String systemPrompt, String userPrompt) {
            return response;
        }
    }
}
