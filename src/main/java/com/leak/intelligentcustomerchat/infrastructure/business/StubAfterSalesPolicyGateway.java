package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.AfterSalesPolicyResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class StubAfterSalesPolicyGateway implements AfterSalesPolicyGateway {
    private static final String SOURCE_SYSTEM = "local-after-sales-policy-catalog";

    private final LocalBusinessDataCatalog businessDataCatalog;

    @Autowired
    public StubAfterSalesPolicyGateway(LocalBusinessDataCatalog businessDataCatalog) {
        this.businessDataCatalog = businessDataCatalog;
    }

    @Override
    public AfterSalesPolicyResult query(BusinessQueryContext context) {
        return businessDataCatalog.findPolicy(context.intent())
                .map(record -> new AfterSalesPolicyResult(
                        GatewayQueryStatus.SUCCESS,
                        SOURCE_SYSTEM,
                        record.policyCode(),
                        record.policyNotes(),
                        List.of(),
                        List.of(),
                        OffsetDateTime.now()
                ))
                .orElseGet(() -> new AfterSalesPolicyResult(
                        GatewayQueryStatus.NO_RESULT,
                        SOURCE_SYSTEM,
                        null,
                        List.of(),
                        List.of(),
                        List.of("policy_not_found_for_intent"),
                        OffsetDateTime.now()
                ));
    }
}
