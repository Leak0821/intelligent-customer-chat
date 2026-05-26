package com.leak.intelligentcustomerchat.infrastructure.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.business.LogisticsQueryResult;

public interface LogisticsQueryGateway {
    LogisticsQueryResult query(BusinessQueryContext context);
}
