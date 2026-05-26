package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

public interface IntentNormalizationTraceService {
    IntentNormalizationDiagnostics diagnose(InboundMail mail);
}
