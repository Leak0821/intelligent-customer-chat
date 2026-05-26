package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

public interface ContextLoadingTraceService {
    ContextLoadingDiagnostics diagnose(InboundMail mail, IntentRouteResult routeResult);
}
