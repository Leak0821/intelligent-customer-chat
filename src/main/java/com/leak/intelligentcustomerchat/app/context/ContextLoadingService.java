package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

public interface ContextLoadingService {
    ContextSnapshot load(InboundMail mail, IntentRouteResult routeResult);
}
