package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.infrastructure.business.StubAfterSalesPolicyGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubLogisticsQueryGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubOrderQueryGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBusinessFactServiceTest {
    private final DefaultBusinessFactService service = new DefaultBusinessFactService(
            new StubOrderQueryGateway(),
            new StubLogisticsQueryGateway(),
            new StubAfterSalesPolicyGateway()
    );

    @Test
    void shouldReturnNotRequiredForPreSales() {
        BusinessFactResult result = service.loadFacts(
                new InboundMail("msg-1", "thread-1", "buyer@example.com", "Need recommendation", "recommend a lamp", java.time.OffsetDateTime.now()),
                new IntentNormalizationResult(
                        "recommend a lamp",
                        "recommend a lamp",
                        List.of(),
                        List.of(CustomerScene.PRE_SALES),
                        List.of("product_recommendation"),
                        List.of(),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.PRE_SALES, "product_recommendation", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("thread=t-1", List.of(), List.of())
        );

        assertThat(result.status()).isEqualTo(BusinessFactStatus.NOT_REQUIRED);
    }

    @Test
    void shouldMergeOrderAndLogisticsFactsForAfterSalesTracking() {
        BusinessFactResult result = service.loadFacts(
                new InboundMail("msg-2", "thread-2", "buyer@example.com", "Track order", "Please check order #ABCD1234 and tracking number ZXCV9876", java.time.OffsetDateTime.now()),
                new IntentNormalizationResult(
                        "Please check order #ABCD1234 and tracking number ZXCV9876",
                        "Please check order #ABCD1234 and tracking number ZXCV9876",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("logistics_tracking"),
                        List.of("order_id_or_tracking_no"),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "logistics_tracking", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("thread=t-2", List.of(), List.of())
        );

        assertThat(result.status()).isEqualTo(BusinessFactStatus.SUCCESS);
        assertThat(result.sourceSystem()).contains("stub-order-gateway");
        assertThat(result.sourceSystem()).contains("stub-logistics-gateway");
        assertThat(result.resolvedEntities()).anyMatch(item -> item.contains("ABCD1234"));
        assertThat(result.resolvedEntities()).anyMatch(item -> item.contains("ZXCV9876"));
        assertThat(result.facts()).anyMatch(item -> item.contains("latest logistics node"));
    }

    @Test
    void shouldIncludeStructuredPolicyFactsForAfterSalesPolicyIntent() {
        BusinessFactResult result = service.loadFacts(
                new InboundMail("msg-3", "thread-3", "buyer@example.com", "Refund policy", "Please check order #EFGH5678 and tell me the refund policy", java.time.OffsetDateTime.now()),
                new IntentNormalizationResult(
                        "Please check order #EFGH5678 and tell me the refund policy",
                        "Please check order #EFGH5678 and tell me the refund policy",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("after_sales_policy"),
                        List.of("order_id_or_tracking_no"),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "after_sales_policy", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("thread=t-3", List.of(), List.of())
        );

        assertThat(result.status()).isEqualTo(BusinessFactStatus.SUCCESS);
        assertThat(result.sourceSystem()).contains("stub-after-sales-policy-gateway");
        assertThat(result.resolvedEntities()).contains("policy_code=AFTER_SALES_STANDARD_POLICY");
        assertThat(result.facts()).anyMatch(item -> item.contains("verify order facts before promising compensation"));
    }
}
