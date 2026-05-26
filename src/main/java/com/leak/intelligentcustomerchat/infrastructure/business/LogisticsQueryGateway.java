package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;

public interface LogisticsQueryGateway {
    BusinessFactResult query(BusinessQueryContext context);
}
