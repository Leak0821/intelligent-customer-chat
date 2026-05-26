package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.AfterSalesPolicyResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class StubAfterSalesPolicyGateway implements AfterSalesPolicyGateway {

    @Override
    public AfterSalesPolicyResult query(BusinessQueryContext context) {
        return new AfterSalesPolicyResult(
                GatewayQueryStatus.SUCCESS,
                "stub-after-sales-policy-gateway",
                "AFTER_SALES_STANDARD_POLICY",
                List.of(
                        "standard policy note=Please verify order facts before promising compensation",
                        "standard policy note=Return or refund requests should move to manual review if customer asks for exception"
                ),
                List.of(),
                List.of(),
                OffsetDateTime.now()
        );
    }
}
