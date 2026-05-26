package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

@Service
public class IntentHeuristicPreviewService {
    private final IntentNormalizationHeuristics heuristics = new IntentNormalizationHeuristics();

    public IntentNormalizationResult preview(InboundMail mail) {
        return heuristics.normalize(mail);
    }
}
