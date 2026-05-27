package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.app.context.ContextEntitySignalExtractor;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.infrastructure.business.LocalBusinessDataCatalog;
import com.leak.intelligentcustomerchat.infrastructure.business.StubAfterSalesPolicyGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubLogisticsQueryGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubOrderQueryGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBusinessFactServiceTest {
    private final LocalBusinessDataCatalog businessDataCatalog = new LocalBusinessDataCatalog();
    private final DefaultBusinessFactService service = new DefaultBusinessFactService(
            new StubOrderQueryGateway(businessDataCatalog),
            new StubLogisticsQueryGateway(businessDataCatalog),
            new StubAfterSalesPolicyGateway(businessDataCatalog),
            new ContextEntitySignalExtractor()
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
        assertThat(result.sourceSystem()).contains("local-order-catalog");
        assertThat(result.sourceSystem()).contains("local-logistics-catalog");
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
        assertThat(result.sourceSystem()).contains("local-order-catalog");
        assertThat(result.sourceSystem()).contains("local-after-sales-policy-catalog");
        assertThat(result.resolvedEntities()).contains("policy_code=AFTER_SALES_STANDARD_POLICY");
        assertThat(result.facts()).anyMatch(item -> item.contains("verify order facts before promising compensation"));
    }

    @Test
    void shouldExtractOrderNumberWhenMailSaysOrderNumberIs() {
        BusinessFactResult result = service.loadFacts(
                new InboundMail("msg-3b", "thread-3b", "buyer@example.com", "Order status", "My order number is EFGH5678 and I want to know when it will ship.", java.time.OffsetDateTime.now()),
                new IntentNormalizationResult(
                        "My order number is EFGH5678 and I want to know when it will ship.",
                        "My order number is EFGH5678 and I want to know when it will ship.",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("order_status"),
                        List.of("order_id_or_tracking_no"),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "order_status", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("thread=t-3b", List.of(), List.of())
        );

        assertThat(result.status()).isEqualTo(BusinessFactStatus.SUCCESS);
        assertThat(result.resolvedEntities()).contains("order_id=EFGH5678");
        assertThat(result.facts()).anyMatch(item -> item.contains("order status=processing"));
    }

    @Test
    void shouldReturnConflictWhenOrderDoesNotBelongToCurrentCustomer() {
        BusinessFactResult result = service.loadFacts(
                new InboundMail("msg-4", "thread-4", "intruder@example.com", "Track order", "Please check order #ABCD1234", java.time.OffsetDateTime.now()),
                new IntentNormalizationResult(
                        "Please check order #ABCD1234",
                        "Please check order #ABCD1234",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("order_status"),
                        List.of("order_id_or_tracking_no"),
                        List.of(),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "order_status", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("thread=t-4", List.of(), List.of())
        );

        assertThat(result.status()).isEqualTo(BusinessFactStatus.CONFLICT);
        assertThat(result.conflictFlags()).contains("order_customer_email_mismatch");
    }

    @Test
    void shouldReuseOrderIdentifierFromContextWhenCurrentMailOmitsIt() {
        BusinessFactResult result = service.loadFacts(
                new InboundMail("msg-5", "thread-5", "buyer@example.com", "Order status", "Can you check the latest order status again?", java.time.OffsetDateTime.now()),
                new IntentNormalizationResult(
                        "Can you check the latest order status again?",
                        "Can you check the latest order status again?",
                        List.of(),
                        List.of(CustomerScene.AFTER_SALES),
                        List.of("order_status"),
                        List.of("order_id_or_tracking_no"),
                        List.of("order_id_or_tracking_no"),
                        ProcessingDisposition.CONTINUE
                ),
                new IntentRouteResult(CustomerScene.AFTER_SALES, "order_status", ProcessingDisposition.CONTINUE, "test"),
                new ContextSnapshot("previous mail already confirmed order", List.of("order_id=EFGH5678"), List.of())
        );

        assertThat(result.status()).isEqualTo(BusinessFactStatus.SUCCESS);
        assertThat(result.resolvedEntities()).contains("order_id=EFGH5678");
        assertThat(result.facts()).anyMatch(item -> item.contains("order status=processing"));
    }
}
