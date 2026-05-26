package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;

public interface AfterSalesPolicyGateway {
    BusinessFactResult query(BusinessQueryContext context);
}
