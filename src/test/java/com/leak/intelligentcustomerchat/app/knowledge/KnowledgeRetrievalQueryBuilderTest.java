package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalQueryBuilderTest {
    private final KnowledgeRetrievalQueryBuilder builder = new KnowledgeRetrievalQueryBuilder();

    @Test
    void shouldBuildRouteAwareQueryWithContextAndFacts() {
        var query = builder.build(
                new IntentNormalizationResult(
                        "Customer wants the latest logistics update for order ABCD1234.",
                        "Where is my package now?",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("logistics_tracking"),
                        List.of("order_id_or_tracking_no"),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "logistics_tracking", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("customer asked again after previous email", List.of("tracking_number=ZXCV9876"), List.of()),
                new BusinessFactResult(
                        BusinessFactStatus.SUCCESS,
                        "stub",
                        List.of("tracking_number=ZXCV9876"),
                        List.of("current logistics status=in_transit", "latest logistics node=Departed regional hub"),
                        List.of(),
                        List.of(),
                        OffsetDateTime.now()
                ),
                new RetrievalSettingsConfig(10, true, 60)
        );

        assertThat(query.queryText()).contains("Where is my package now?");
        assertThat(query.queryText()).contains("logistics tracking delivery update shipping timeline");
        assertThat(query.queryText()).contains("context customer asked again after previous email");
        assertThat(query.queryText()).contains("facts current logistics status=in_transit");
        assertThat(query.filters()).containsExactly("tracking_number=ZXCV9876");
        assertThat(query.topK()).isEqualTo(10);
    }

    @Test
    void shouldFallbackToSubIntentWhenNoOtherSignalExists() {
        var query = builder.build(
                new IntentNormalizationResult(
                        "",
                        "",
                        List.of(),
                        List.of(CustomerScene.PRE_SALES),
                        List.of("general_inquiry"),
                        List.of(),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.PRE_SALES, "general_inquiry", ProcessingDisposition.CONTINUE, "test"),
                ContextSnapshot.empty(),
                BusinessFactResult.notRequired(),
                new RetrievalSettingsConfig(5, true, 60)
        );

        assertThat(query.queryText()).isEqualTo("general inquiry");
        assertThat(query.filters()).isEmpty();
        assertThat(query.topK()).isEqualTo(5);
    }
}
