package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultBusinessFactService implements BusinessFactService {

    @Override
    public BusinessFactResult loadFacts(IntentNormalizationResult normalizationResult,
                                        IntentRouteResult routeResult,
                                        ContextSnapshot contextSnapshot) {
        if (routeResult.scene() == CustomerScene.PRE_SALES || routeResult.scene() == CustomerScene.UNKNOWN) {
            return new BusinessFactResult(BusinessFactStatus.NOT_REQUIRED, List.of(), List.of());
        }
        if (!normalizationResult.missingEntities().isEmpty()) {
            return new BusinessFactResult(BusinessFactStatus.INSUFFICIENT_INPUT, List.of(), normalizationResult.missingEntities());
        }
        return new BusinessFactResult(
                BusinessFactStatus.SUCCESS,
                List.of("order lookup placeholder resolved for " + routeResult.subIntent()),
                List.of()
        );
    }
}
