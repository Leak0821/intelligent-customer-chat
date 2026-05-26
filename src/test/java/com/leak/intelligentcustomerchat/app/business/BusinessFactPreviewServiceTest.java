package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import com.leak.intelligentcustomerchat.infrastructure.business.LocalBusinessDataCatalog;
import com.leak.intelligentcustomerchat.infrastructure.business.StubAfterSalesPolicyGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubLogisticsQueryGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubOrderQueryGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessFactPreviewServiceTest {
    private final LocalBusinessDataCatalog businessDataCatalog = new LocalBusinessDataCatalog();
    private final BusinessFactPreviewService service = new BusinessFactPreviewService(
            new StubOrderQueryGateway(businessDataCatalog),
            new StubLogisticsQueryGateway(businessDataCatalog),
            new StubAfterSalesPolicyGateway(businessDataCatalog)
    );

    @Test
    void shouldPreviewOrderAndLogisticsForTrackingIntent() {
        BusinessFactPreviewView view = service.preview(new BusinessQueryContext(
                "buyer@example.com",
                "thread-1",
                "AFTER_SALES",
                "logistics_tracking",
                "ABCD1234",
                "ZXCV9876",
                "manual logistics preview"
        ));

        assertThat(view.queriedGateways()).containsExactly("order", "logistics");
        assertThat(view.orderResult()).isNotNull();
        assertThat(view.logisticsResult()).isNotNull();
        assertThat(view.policyResult()).isNull();
        assertThat(view.orderResult().status()).isEqualTo(GatewayQueryStatus.SUCCESS);
        assertThat(view.logisticsResult().status()).isEqualTo(GatewayQueryStatus.SUCCESS);
    }

    @Test
    void shouldPreviewPolicyForAfterSalesPolicyIntent() {
        BusinessFactPreviewView view = service.preview(new BusinessQueryContext(
                "buyer@example.com",
                "thread-2",
                "AFTER_SALES",
                "after_sales_policy",
                "EFGH5678",
                null,
                "manual policy preview"
        ));

        assertThat(view.queriedGateways()).containsExactly("order", "policy");
        assertThat(view.orderResult()).isNotNull();
        assertThat(view.policyResult()).isNotNull();
        assertThat(view.policyResult().status()).isEqualTo(GatewayQueryStatus.SUCCESS);
        assertThat(view.policyResult().policyCode()).isEqualTo("AFTER_SALES_STANDARD_POLICY");
    }
}
