package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.OrderQueryResult;

public interface OrderQueryGateway {
    OrderQueryResult query(BusinessQueryContext context);
}
