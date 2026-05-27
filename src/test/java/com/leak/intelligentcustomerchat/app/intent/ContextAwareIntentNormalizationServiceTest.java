package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.app.context.ContextEntitySignalExtractor;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAwareIntentNormalizationServiceTest {
    private final ContextAwareIntentNormalizationService service =
            new ContextAwareIntentNormalizationService(new ContextEntitySignalExtractor());

    @Test
    void shouldReuseContextIdentifierAndDowngradeFollowUpToContinue() {
        IntentNormalizationResult normalizationResult = new IntentNormalizationResult(
                "Can you check the latest order status again?",
                "Can you check the latest order status again?",
                List.of(),
                List.of(CustomerScene.AFTER_SALES),
                List.of("order_status"),
                List.of("order_id_or_tracking_no"),
                List.of("order_id_or_tracking_no"),
                ProcessingDisposition.FOLLOW_UP
        );
        ContextSnapshot contextSnapshot = new ContextSnapshot(
                "customer already shared order details previously",
                List.of("order_id=EFGH5678", "tracking_number=TRACK5678"),
                List.of()
        );

        ContextAwareIntentNormalizationResult result = service.enrich(normalizationResult, contextSnapshot);

        assertThat(result.result().missingEntities()).isEmpty();
        assertThat(result.result().disposition()).isEqualTo(ProcessingDisposition.CONTINUE);
        assertThat(result.actions()).containsExactly("reuse_context_order_id", "reuse_context_tracking_number");
    }

    @Test
    void shouldKeepOriginalResultWhenContextHasNoReusableIdentifier() {
        IntentNormalizationResult normalizationResult = new IntentNormalizationResult(
                "Can you check the latest order status again?",
                "Can you check the latest order status again?",
                List.of(),
                List.of(CustomerScene.AFTER_SALES),
                List.of("order_status"),
                List.of("order_id_or_tracking_no"),
                List.of("order_id_or_tracking_no"),
                ProcessingDisposition.FOLLOW_UP
        );

        ContextAwareIntentNormalizationResult result = service.enrich(
                normalizationResult,
                new ContextSnapshot("customer asked again", List.of(), List.of("please check again"))
        );

        assertThat(result.result()).isEqualTo(normalizationResult);
        assertThat(result.actions()).isEmpty();
    }
}
