package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.AfterSalesPolicyResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.LogisticsQueryResult;
import com.leak.intelligentcustomerchat.domain.business.OrderQueryResult;

import java.util.List;

public record BusinessFactPreviewView(
        String scene,
        String subIntent,
        BusinessQueryContext queryContext,
        List<String> queriedGateways,
        OrderQueryResult orderResult,
        LogisticsQueryResult logisticsResult,
        AfterSalesPolicyResult policyResult
) {
}
