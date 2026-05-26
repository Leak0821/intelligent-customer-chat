package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.business.BusinessFactPreviewService;
import com.leak.intelligentcustomerchat.app.business.BusinessFactPreviewView;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.infrastructure.business.AfterSalesPolicyCatalogRecord;
import com.leak.intelligentcustomerchat.infrastructure.business.LocalBusinessDataCatalog;
import com.leak.intelligentcustomerchat.infrastructure.business.LogisticsCatalogRecord;
import com.leak.intelligentcustomerchat.infrastructure.business.OrderCatalogRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/business")
public class BusinessDataAdminController {
    private final LocalBusinessDataCatalog businessDataCatalog;
    private final BusinessFactPreviewService businessFactPreviewService;

    public BusinessDataAdminController(LocalBusinessDataCatalog businessDataCatalog,
                                       BusinessFactPreviewService businessFactPreviewService) {
        this.businessDataCatalog = businessDataCatalog;
        this.businessFactPreviewService = businessFactPreviewService;
    }

    @GetMapping("/orders")
    public List<OrderCatalogRecord> listOrders() {
        return businessDataCatalog.listOrders();
    }

    @GetMapping("/orders/{orderId}")
    public OrderCatalogRecord getOrder(@PathVariable String orderId) {
        return businessDataCatalog.findOrder(orderId)
                .orElseThrow(() -> new NoSuchElementException("order not found: " + orderId));
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

    @GetMapping("/logistics/{trackingNumber}")
    public LogisticsCatalogRecord getLogistics(@PathVariable String trackingNumber) {
        return businessDataCatalog.findLogistics(null, trackingNumber)
                .orElseThrow(() -> new NoSuchElementException("logistics not found: " + trackingNumber));
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

    @GetMapping("/policies/{policyCode}")
    public AfterSalesPolicyCatalogRecord getPolicy(@PathVariable String policyCode) {
        return businessDataCatalog.findPolicyByCode(policyCode)
                .orElseThrow(() -> new NoSuchElementException("policy not found: " + policyCode));
    }

    @GetMapping("/policies/by-intent/{intent}")
    public AfterSalesPolicyCatalogRecord getPolicyByIntent(@PathVariable String intent) {
        return businessDataCatalog.findPolicy(intent)
                .orElseThrow(() -> new NoSuchElementException("policy not found for intent: " + intent));
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

    @PostMapping("/facts/preview")
    public BusinessFactPreviewView previewFacts(@RequestBody BusinessFactPreviewRequest request) {
        BusinessQueryContext queryContext = new BusinessQueryContext(
                request.customerEmail(),
                request.threadId() == null || request.threadId().isBlank() ? UUID.randomUUID().toString() : request.threadId(),
                request.scene() == null || request.scene().isBlank() ? "AFTER_SALES" : request.scene().trim().toUpperCase(Locale.ROOT),
                request.subIntent() == null || request.subIntent().isBlank()
                        ? "general_inquiry"
                        : request.subIntent().trim().toLowerCase(Locale.ROOT),
                request.orderId(),
                request.trackingNumber(),
                request.queryReason() == null || request.queryReason().isBlank()
                        ? "manual preview from business admin endpoint"
                        : request.queryReason()
        );
        return businessFactPreviewService.preview(queryContext);
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

    public record BusinessFactPreviewRequest(
            String customerEmail,
            String threadId,
            String scene,
            String subIntent,
            String orderId,
            String trackingNumber,
            String queryReason
    ) {
    }
}
