package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.AfterSalesPolicyResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;

public interface AfterSalesPolicyGateway {
    AfterSalesPolicyResult query(BusinessQueryContext context);
}
