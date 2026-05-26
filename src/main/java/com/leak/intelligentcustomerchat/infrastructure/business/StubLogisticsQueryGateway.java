package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import com.leak.intelligentcustomerchat.domain.business.LogisticsQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Component
public class StubLogisticsQueryGateway implements LogisticsQueryGateway {
    private static final String SOURCE_SYSTEM = "local-logistics-catalog";

    private final LocalBusinessDataCatalog businessDataCatalog;

    @Autowired
    public StubLogisticsQueryGateway(LocalBusinessDataCatalog businessDataCatalog) {
        this.businessDataCatalog = businessDataCatalog;
    }

    @Override
    public LogisticsQueryResult query(BusinessQueryContext context) {
        String key = context.trackingNumber() == null || context.trackingNumber().isBlank()
                ? context.orderId()
                : context.trackingNumber();
        if (key == null || key.isBlank()) {
            return LogisticsQueryResult.insufficientInput(SOURCE_SYSTEM, List.of("order_id_or_tracking_number"));
        }
        return businessDataCatalog.findLogistics(context.orderId(), context.trackingNumber())
                .map(record -> toResult(record, context))
                .orElseGet(() -> new LogisticsQueryResult(
                        GatewayQueryStatus.NO_RESULT,
                        SOURCE_SYSTEM,
                        context.orderId(),
                        context.trackingNumber(),
                        null,
                        null,
                        null,
                        List.of(),
                        List.of("logistics_record_not_found"),
                        OffsetDateTime.now()
                ));
    }

    private LogisticsQueryResult toResult(LogisticsCatalogRecord record, BusinessQueryContext context) {
        if (context.orderId() != null
                && !context.orderId().isBlank()
                && !context.orderId().equals(record.orderId())) {
            return new LogisticsQueryResult(
                    GatewayQueryStatus.CONFLICT,
                    SOURCE_SYSTEM,
                    record.orderId(),
                    record.trackingNumber(),
                    record.logisticsStatus(),
                    record.latestNode(),
                    record.etaHint(),
                    List.of(),
                    List.of("tracking_order_mismatch"),
                    OffsetDateTime.now()
            );
        }
        if (isCustomerEmailConflict(record.orderId(), context.customerEmail())) {
            return new LogisticsQueryResult(
                    GatewayQueryStatus.CONFLICT,
                    SOURCE_SYSTEM,
                    record.orderId(),
                    record.trackingNumber(),
                    record.logisticsStatus(),
                    record.latestNode(),
                    record.etaHint(),
                    List.of(),
                    List.of("logistics_customer_email_mismatch"),
                    OffsetDateTime.now()
            );
        }
        return new LogisticsQueryResult(
                GatewayQueryStatus.SUCCESS,
                SOURCE_SYSTEM,
                record.orderId(),
                record.trackingNumber(),
                record.logisticsStatus(),
                record.latestNode(),
                record.etaHint(),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }

    private boolean isCustomerEmailConflict(String orderId, String customerEmail) {
        return businessDataCatalog.findOrder(orderId)
                .map(order -> !order.customerEmail().toLowerCase(Locale.ROOT).equals(customerEmail.toLowerCase(Locale.ROOT)))
                .orElse(false);
    }
}
