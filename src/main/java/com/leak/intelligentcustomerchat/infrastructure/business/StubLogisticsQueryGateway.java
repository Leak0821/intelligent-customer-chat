package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import com.leak.intelligentcustomerchat.domain.business.LogisticsQueryResult;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class StubLogisticsQueryGateway implements LogisticsQueryGateway {

    @Override
    public LogisticsQueryResult query(BusinessQueryContext context) {
        String key = context.trackingNumber() == null || context.trackingNumber().isBlank()
                ? context.orderId()
                : context.trackingNumber();
        if (key == null || key.isBlank()) {
            return LogisticsQueryResult.insufficientInput("stub-logistics-gateway", List.of("order_id_or_tracking_number"));
        }
        return new LogisticsQueryResult(
                GatewayQueryStatus.SUCCESS,
                "stub-logistics-gateway",
                context.orderId(),
                context.trackingNumber(),
                "in_transit",
                "Departed regional hub",
                "estimated next update within 24 hours",
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }
}
