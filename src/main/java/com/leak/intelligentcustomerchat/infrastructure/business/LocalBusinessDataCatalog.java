package com.leak.intelligentcustomerchat.infrastructure.business;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalBusinessDataCatalog {
    private final Map<String, OrderCatalogRecord> ordersById = new ConcurrentHashMap<>();
    private final Map<String, LogisticsCatalogRecord> logisticsByTracking = new ConcurrentHashMap<>();
    private final Map<String, String> trackingByOrderId = new ConcurrentHashMap<>();
    private final Map<String, AfterSalesPolicyCatalogRecord> policiesByCode = new ConcurrentHashMap<>();

    public LocalBusinessDataCatalog() {
        seedDefaults();
    }

    public List<OrderCatalogRecord> listOrders() {
        return ordersById.values().stream()
                .sorted(Comparator.comparing(OrderCatalogRecord::orderId))
                .toList();
    }

    public OrderCatalogRecord upsertOrder(OrderCatalogRecord record) {
        ordersById.put(record.orderId(), record);
        if (!record.trackingNumber().isBlank()) {
            trackingByOrderId.put(record.orderId(), record.trackingNumber());
        }
        return record;
    }

    public Optional<OrderCatalogRecord> findOrder(String orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }

    public List<LogisticsCatalogRecord> listLogistics() {
        return logisticsByTracking.values().stream()
                .sorted(Comparator.comparing(LogisticsCatalogRecord::trackingNumber))
                .toList();
    }

    public LogisticsCatalogRecord upsertLogistics(LogisticsCatalogRecord record) {
        logisticsByTracking.put(record.trackingNumber(), record);
        trackingByOrderId.put(record.orderId(), record.trackingNumber());
        return record;
    }

    public Optional<LogisticsCatalogRecord> findLogistics(String orderId, String trackingNumber) {
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            LogisticsCatalogRecord byTracking = logisticsByTracking.get(trackingNumber);
            if (byTracking != null) {
                return Optional.of(byTracking);
            }
        }
        if (orderId == null || orderId.isBlank()) {
            return Optional.empty();
        }
        String resolvedTracking = trackingByOrderId.get(orderId);
        if (resolvedTracking == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(logisticsByTracking.get(resolvedTracking));
    }

    public List<AfterSalesPolicyCatalogRecord> listPolicies() {
        return policiesByCode.values().stream()
                .sorted(Comparator.comparing(AfterSalesPolicyCatalogRecord::policyCode))
                .toList();
    }

    public AfterSalesPolicyCatalogRecord upsertPolicy(AfterSalesPolicyCatalogRecord record) {
        policiesByCode.put(record.policyCode(), record);
        return record;
    }

    public Optional<AfterSalesPolicyCatalogRecord> findPolicy(String intent) {
        return policiesByCode.values().stream()
                .filter(AfterSalesPolicyCatalogRecord::enabled)
                .filter(record -> record.applicableIntents().isEmpty() || record.applicableIntents().contains(intent))
                .sorted(Comparator.comparing(AfterSalesPolicyCatalogRecord::policyCode))
                .findFirst();
    }

    private void seedDefaults() {
        OffsetDateTime now = OffsetDateTime.now();
        upsertOrder(new OrderCatalogRecord("ABCD1234", "buyer@example.com", "shipped", "paid", "ZXCV9876", now.minusDays(3)));
        upsertOrder(new OrderCatalogRecord("EFGH5678", "buyer@example.com", "processing", "paid", "TRACK5678", now.minusDays(1)));
        upsertOrder(new OrderCatalogRecord("MNOP2468", "customer@example.com", "delivered", "paid", "DELI2468", now.minusDays(8)));

        upsertLogistics(new LogisticsCatalogRecord("ZXCV9876", "ABCD1234", "in_transit", "Departed regional hub", "estimated next update within 24 hours", now.minusHours(6)));
        upsertLogistics(new LogisticsCatalogRecord("TRACK5678", "EFGH5678", "label_created", "Carrier label created", "carrier pickup expected within 12 hours", now.minusHours(2)));
        upsertLogistics(new LogisticsCatalogRecord("DELI2468", "MNOP2468", "delivered", "Delivered to front door", "delivered on schedule", now.minusDays(1)));

        upsertPolicy(new AfterSalesPolicyCatalogRecord(
                "AFTER_SALES_STANDARD_POLICY",
                "Standard after-sales policy",
                List.of("after_sales_policy", "order_status", "logistics_tracking"),
                List.of(
                        "standard policy note=Please verify order facts before promising compensation",
                        "standard policy note=Return or refund requests should move to manual review if customer asks for exception"
                ),
                true,
                now.minusDays(2)
        ));
        upsertPolicy(new AfterSalesPolicyCatalogRecord(
                "DELIVERY_DELAY_POLICY",
                "Delivery delay escalation policy",
                List.of("logistics_tracking"),
                List.of(
                        "delay policy note=If the parcel has not moved for more than 72 hours, suggest manual logistics follow-up",
                        "delay policy note=Do not promise refund before carrier exception is confirmed"
                ),
                true,
                now.minusDays(1)
        ));
    }
}
