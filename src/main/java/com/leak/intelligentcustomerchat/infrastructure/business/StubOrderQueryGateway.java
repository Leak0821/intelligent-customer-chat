package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class StubOrderQueryGateway implements OrderQueryGateway {

    @Override
    public BusinessFactResult query(BusinessQueryContext context) {
        if (context.orderId() == null || context.orderId().isBlank()) {
            return BusinessFactResult.insufficientInput(List.of("order_id"));
        }
        String orderStatus = deriveOrderStatus(context.orderId());
        return new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                "stub-order-gateway",
                List.of("order_id=" + context.orderId()),
                List.of(
                        "order status=" + orderStatus,
                        "payment status=paid",
                        "order owner email=" + context.customerEmail()
                ),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }

    private String deriveOrderStatus(String orderId) {
        char lastChar = orderId.charAt(orderId.length() - 1);
        if (Character.isDigit(lastChar) && ((lastChar - '0') % 2 == 0)) {
            return "shipped";
        }
        return "processing";
    }
}
