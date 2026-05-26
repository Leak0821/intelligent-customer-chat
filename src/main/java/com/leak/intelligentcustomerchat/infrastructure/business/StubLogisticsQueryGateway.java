package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class StubLogisticsQueryGateway implements LogisticsQueryGateway {

    @Override
    public BusinessFactResult query(BusinessQueryContext context) {
        String key = context.trackingNumber() == null || context.trackingNumber().isBlank()
                ? context.orderId()
                : context.trackingNumber();
        if (key == null || key.isBlank()) {
            return new BusinessFactResult(
                    BusinessFactStatus.INSUFFICIENT_INPUT,
                    "stub-logistics-gateway",
                    List.of(),
                    List.of(),
                    List.of("order_id_or_tracking_number"),
                    List.of(),
                    OffsetDateTime.now()
            );
        }

        List<String> resolved = new ArrayList<>();
        if (context.orderId() != null && !context.orderId().isBlank()) {
            resolved.add("order_id=" + context.orderId());
        }
        if (context.trackingNumber() != null && !context.trackingNumber().isBlank()) {
            resolved.add("tracking_number=" + context.trackingNumber());
        }
        return new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                "stub-logistics-gateway",
                resolved,
                List.of(
                        "latest logistics node=Departed regional hub",
                        "estimated next update within 24 hours",
                        "current logistics status=in_transit"
                ),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }
}
