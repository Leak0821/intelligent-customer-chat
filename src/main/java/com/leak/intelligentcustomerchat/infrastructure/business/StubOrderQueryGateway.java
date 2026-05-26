package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import com.leak.intelligentcustomerchat.domain.business.OrderQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Component
public class StubOrderQueryGateway implements OrderQueryGateway {
    private static final String SOURCE_SYSTEM = "local-order-catalog";

    private final LocalBusinessDataCatalog businessDataCatalog;

    @Autowired
    public StubOrderQueryGateway(LocalBusinessDataCatalog businessDataCatalog) {
        this.businessDataCatalog = businessDataCatalog;
    }

    @Override
    public OrderQueryResult query(BusinessQueryContext context) {
        if (context.orderId() == null || context.orderId().isBlank()) {
            return OrderQueryResult.insufficientInput(SOURCE_SYSTEM, List.of("order_id"));
        }
        return businessDataCatalog.findOrder(context.orderId())
                .map(record -> toSuccessResult(record, context.customerEmail()))
                .orElseGet(() -> new OrderQueryResult(
                        GatewayQueryStatus.NO_RESULT,
                        SOURCE_SYSTEM,
                        context.orderId(),
                        null,
                        null,
                        null,
                        List.of(),
                        List.of("order_not_found"),
                        OffsetDateTime.now()
                ));
    }

    private OrderQueryResult toSuccessResult(OrderCatalogRecord record, String customerEmail) {
        if (!record.customerEmail().toLowerCase(Locale.ROOT).equals(customerEmail.toLowerCase(Locale.ROOT))) {
            return new OrderQueryResult(
                    GatewayQueryStatus.CONFLICT,
                    SOURCE_SYSTEM,
                    record.orderId(),
                    record.orderStatus(),
                    record.paymentStatus(),
                    record.customerEmail(),
                    List.of(),
                    List.of("order_customer_email_mismatch"),
                    OffsetDateTime.now()
            );
        }
        return new OrderQueryResult(
                GatewayQueryStatus.SUCCESS,
                SOURCE_SYSTEM,
                record.orderId(),
                record.orderStatus(),
                record.paymentStatus(),
                record.customerEmail(),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }
}
