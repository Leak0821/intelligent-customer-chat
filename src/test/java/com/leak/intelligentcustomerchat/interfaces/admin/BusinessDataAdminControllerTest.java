package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.infrastructure.business.LocalBusinessDataCatalog;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDataAdminControllerTest {

    @Test
    void shouldListAndUpsertBusinessDataCatalogRecords() {
        BusinessDataAdminController controller = new BusinessDataAdminController(new LocalBusinessDataCatalog());

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
    }
}
