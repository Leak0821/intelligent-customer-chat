package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

public interface IntentNormalizationService {
    IntentNormalizationResult normalize(InboundMail mail);
}
