package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.infrastructure.business.AfterSalesPolicyCatalogRecord;
import com.leak.intelligentcustomerchat.infrastructure.business.LocalBusinessDataCatalog;
import com.leak.intelligentcustomerchat.infrastructure.business.LogisticsCatalogRecord;
import com.leak.intelligentcustomerchat.infrastructure.business.OrderCatalogRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/business")
public class BusinessDataAdminController {
    private final LocalBusinessDataCatalog businessDataCatalog;

    public BusinessDataAdminController(LocalBusinessDataCatalog businessDataCatalog) {
        this.businessDataCatalog = businessDataCatalog;
    }

    @GetMapping("/orders")
    public List<OrderCatalogRecord> listOrders() {
        return businessDataCatalog.listOrders();
    }

    @PostMapping("/orders")
    public OrderCatalogRecord upsertOrder(@RequestBody UpsertOrderRequest request) {
        return businessDataCatalog.upsertOrder(new OrderCatalogRecord(
                request.orderId(),
                request.customerEmail(),
                request.orderStatus(),
                request.paymentStatus(),
                request.trackingNumber(),
                request.orderCreatedAt() == null ? OffsetDateTime.now() : request.orderCreatedAt()
        ));
    }

    @GetMapping("/logistics")
    public List<LogisticsCatalogRecord> listLogistics() {
        return businessDataCatalog.listLogistics();
    }

    @PostMapping("/logistics")
    public LogisticsCatalogRecord upsertLogistics(@RequestBody UpsertLogisticsRequest request) {
        return businessDataCatalog.upsertLogistics(new LogisticsCatalogRecord(
                request.trackingNumber(),
                request.orderId(),
                request.logisticsStatus(),
                request.latestNode(),
                request.etaHint(),
                request.updatedAt() == null ? OffsetDateTime.now() : request.updatedAt()
        ));
    }

    @GetMapping("/policies")
    public List<AfterSalesPolicyCatalogRecord> listPolicies() {
        return businessDataCatalog.listPolicies();
    }

    @PostMapping("/policies")
    public AfterSalesPolicyCatalogRecord upsertPolicy(@RequestBody UpsertPolicyRequest request) {
        return businessDataCatalog.upsertPolicy(new AfterSalesPolicyCatalogRecord(
                request.policyCode(),
                request.policyName(),
                request.applicableIntents(),
                request.policyNotes(),
                request.enabled(),
                request.updatedAt() == null ? OffsetDateTime.now() : request.updatedAt()
        ));
    }

    public record UpsertOrderRequest(
            String orderId,
            String customerEmail,
            String orderStatus,
            String paymentStatus,
            String trackingNumber,
            OffsetDateTime orderCreatedAt
    ) {
    }

    public record UpsertLogisticsRequest(
            String trackingNumber,
            String orderId,
            String logisticsStatus,
            String latestNode,
            String etaHint,
            OffsetDateTime updatedAt
    ) {
    }

    public record UpsertPolicyRequest(
            String policyCode,
            String policyName,
            List<String> applicableIntents,
            List<String> policyNotes,
            boolean enabled,
            OffsetDateTime updatedAt
    ) {
    }
}
