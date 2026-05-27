package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.AfterSalesPolicyResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.LogisticsQueryResult;
import com.leak.intelligentcustomerchat.domain.business.OrderQueryResult;
import com.leak.intelligentcustomerchat.infrastructure.business.AfterSalesPolicyGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.LogisticsQueryGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.OrderQueryGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BusinessFactPreviewService {
    private final OrderQueryGateway orderQueryGateway;
    private final LogisticsQueryGateway logisticsQueryGateway;
    private final AfterSalesPolicyGateway afterSalesPolicyGateway;

    public BusinessFactPreviewService(OrderQueryGateway orderQueryGateway,
                                      LogisticsQueryGateway logisticsQueryGateway,
                                      AfterSalesPolicyGateway afterSalesPolicyGateway) {
        this.orderQueryGateway = orderQueryGateway;
        this.logisticsQueryGateway = logisticsQueryGateway;
        this.afterSalesPolicyGateway = afterSalesPolicyGateway;
    }

    public BusinessFactPreviewView preview(BusinessQueryContext queryContext) {
        List<String> queriedGateways = new ArrayList<>();
        OrderQueryResult orderResult = null;
        LogisticsQueryResult logisticsResult = null;
        AfterSalesPolicyResult policyResult = null;

        if (requiresOrderFacts(queryContext.intent())) {
            queriedGateways.add("order");
            orderResult = orderQueryGateway.query(queryContext);
        }
        if (requiresLogisticsFacts(queryContext.intent())) {
            queriedGateways.add("logistics");
            logisticsResult = logisticsQueryGateway.query(queryContext);
        }
        if (requiresPolicyFacts(queryContext.intent())) {
            queriedGateways.add("policy");
            policyResult = afterSalesPolicyGateway.query(queryContext);
        }
        if (queriedGateways.isEmpty()) {
            queriedGateways.add("policy");
            policyResult = afterSalesPolicyGateway.query(queryContext);
        }

        return new BusinessFactPreviewView(
                queryContext.scenario(),
                queryContext.intent(),
                queryContext,
                List.copyOf(queriedGateways),
                orderResult,
                logisticsResult,
                policyResult
        );
    }

    private boolean requiresOrderFacts(String subIntent) {
        return "order_status".equals(subIntent)
                || "logistics_tracking".equals(subIntent)
                || "after_sales_policy".equals(subIntent)
                || "return_refund".equals(subIntent);
    }

    private boolean requiresLogisticsFacts(String subIntent) {
        return "logistics_tracking".equals(subIntent);
    }

    private boolean requiresPolicyFacts(String subIntent) {
        return "after_sales_policy".equals(subIntent)
                || "return_refund".equals(subIntent);
    }
}
