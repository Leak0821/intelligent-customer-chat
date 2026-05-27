package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeRetriever;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultKnowledgeRetrieveServiceTest {

    @Test
    void shouldDelegateStructuredQueryToRetriever() {
        CapturingKnowledgeRetriever retriever = new CapturingKnowledgeRetriever();
        RetrievalConfigService retrievalConfigService = () -> new RetrievalSettingsConfig(8, true, 60);
        DefaultKnowledgeRetrieveService service = new DefaultKnowledgeRetrieveService(
                retriever,
                retrievalConfigService,
                new KnowledgeRetrievalQueryBuilder()
        );

        KnowledgeRetrieveResult result = service.retrieve(
                new IntentNormalizationResult(
                        "Customer asks for refund policy after receiving the product.",
                        "What is the refund process for my delivered order?",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("after_sales_policy"),
                        List.of("order_id_or_tracking_no"),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "after_sales_policy", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("customer received the order and wants to know next steps", List.of(), List.of()),
                new BusinessFactResult(
                        BusinessFactStatus.SUCCESS,
                        "stub",
                        List.of("order_id=MNOP2468"),
                        List.of("order status=delivered"),
                        List.of(),
                        List.of(),
                        OffsetDateTime.now()
                )
        );

        assertThat(result).isSameAs(retriever.result);
        assertThat(retriever.lastQuery).isNotNull();
        assertThat(retriever.lastQuery.queryText()).contains("What is the refund process for my delivered order?");
        assertThat(retriever.lastQuery.queryText()).contains("return refund warranty replacement policy process");
        assertThat(retriever.lastQuery.queryText()).contains("context customer received the order and wants to know next steps");
        assertThat(retriever.lastQuery.queryText()).contains("facts order status=delivered");
        assertThat(retriever.lastQuery.filters()).containsExactly("order_id=MNOP2468");
        assertThat(retriever.lastQuery.topK()).isEqualTo(8);
    }

    private static final class CapturingKnowledgeRetriever implements KnowledgeRetriever {
        private final KnowledgeRetrieveResult result = new KnowledgeRetrieveResult(
                "stub",
                List.of(new KnowledgeSnippet("k-1", "title", "content", 1.0d, "stub")),
                1
        );
        private RetrievalQuery lastQuery;

        @Override
        public KnowledgeRetrieveResult retrieve(RetrievalQuery query) {
            this.lastQuery = query;
            return result;
        }
    }
}
