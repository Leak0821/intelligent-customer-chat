package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.business.BusinessFactPreviewService;
import com.leak.intelligentcustomerchat.infrastructure.business.LocalBusinessDataCatalog;
import com.leak.intelligentcustomerchat.infrastructure.business.StubAfterSalesPolicyGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubLogisticsQueryGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.StubOrderQueryGateway;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDataAdminControllerTest {

    @Test
    void shouldListAndUpsertBusinessDataCatalogRecords() {
        LocalBusinessDataCatalog businessDataCatalog = new LocalBusinessDataCatalog();
        BusinessDataAdminController controller = controller(businessDataCatalog);

        assertThat(controller.listOrders()).isNotEmpty();
        assertThat(controller.listLogistics()).isNotEmpty();
        assertThat(controller.listPolicies()).isNotEmpty();

        var order = controller.upsertOrder(new BusinessDataAdminController.UpsertOrderRequest(
                "QRST1357",
                "new-buyer@example.com",
                "processing",
                "paid",
                "NEWTRACK1357",
                OffsetDateTime.parse("2026-05-26T10:00:00+08:00")
        ));
        var logistics = controller.upsertLogistics(new BusinessDataAdminController.UpsertLogisticsRequest(
                "NEWTRACK1357",
                "QRST1357",
                "label_created",
                "Warehouse packed order",
                "carrier pickup expected within 12 hours",
                OffsetDateTime.parse("2026-05-26T11:00:00+08:00")
        ));
        var policy = controller.upsertPolicy(new BusinessDataAdminController.UpsertPolicyRequest(
                "CUSTOM_DELAY_POLICY",
                "Custom delay policy",
                List.of("logistics_tracking"),
                List.of("custom delay note=Escalate when no new scan appears in 48 hours"),
                true,
                OffsetDateTime.parse("2026-05-26T12:00:00+08:00")
        ));

        assertThat(order.orderId()).isEqualTo("QRST1357");
        assertThat(logistics.trackingNumber()).isEqualTo("NEWTRACK1357");
        assertThat(policy.policyCode()).isEqualTo("CUSTOM_DELAY_POLICY");
        assertThat(controller.listOrders()).anyMatch(item -> item.orderId().equals("QRST1357"));
        assertThat(controller.listLogistics()).anyMatch(item -> item.trackingNumber().equals("NEWTRACK1357"));
        assertThat(controller.listPolicies()).anyMatch(item -> item.policyCode().equals("CUSTOM_DELAY_POLICY"));
        assertThat(controller.getOrder("QRST1357").customerEmail()).isEqualTo("new-buyer@example.com");
        assertThat(controller.getLogistics("NEWTRACK1357").orderId()).isEqualTo("QRST1357");
        assertThat(controller.getPolicy("CUSTOM_DELAY_POLICY").policyName()).isEqualTo("Custom delay policy");
    }

    @Test
    void shouldPreviewBusinessFactsAndFindPolicyByIntent() {
        LocalBusinessDataCatalog businessDataCatalog = new LocalBusinessDataCatalog();
        BusinessDataAdminController controller = controller(businessDataCatalog);

        var policy = controller.getPolicyByIntent("logistics_tracking");
        var preview = controller.previewFacts(new BusinessDataAdminController.BusinessFactPreviewRequest(
                "buyer@example.com",
                "preview-thread-1",
                "AFTER_SALES",
                "logistics_tracking",
                "ABCD1234",
                "ZXCV9876",
                "manual logistics preview"
        ));

        assertThat(policy.policyCode()).isEqualTo("AFTER_SALES_STANDARD_POLICY");
        assertThat(preview.queriedGateways()).containsExactly("order", "logistics");
        assertThat(preview.orderResult()).isNotNull();
        assertThat(preview.logisticsResult()).isNotNull();
        assertThat(preview.logisticsResult().trackingNumber()).isEqualTo("ZXCV9876");
    }

    private BusinessDataAdminController controller(LocalBusinessDataCatalog businessDataCatalog) {
        return new BusinessDataAdminController(
                businessDataCatalog,
                new BusinessFactPreviewService(
                        new StubOrderQueryGateway(businessDataCatalog),
                        new StubLogisticsQueryGateway(businessDataCatalog),
                        new StubAfterSalesPolicyGateway(businessDataCatalog)
                )
        );
    }
}
