package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;

import java.util.List;
import java.util.Objects;

public record ContextAwareIntentNormalizationResult(
        IntentNormalizationResult result,
        List<String> actions
) {
    public ContextAwareIntentNormalizationResult {
        Objects.requireNonNull(result, "result must not be null");
        actions = List.copyOf(actions);
    }
}
