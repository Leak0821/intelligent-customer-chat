package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class StubAfterSalesPolicyGateway implements AfterSalesPolicyGateway {

    @Override
    public BusinessFactResult query(BusinessQueryContext context) {
        return new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                "stub-after-sales-policy-gateway",
                List.of(),
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
