package com.leak.intelligentcustomerchat.domain.intent;

import java.util.List;
import java.util.Objects;

public record IntentNormalizationResult(
        String normalizedRequest,
        String primaryQuestion,
        List<String> secondaryQuestions,
        List<CustomerScene> sceneCandidates,
        List<String> subIntentCandidates,
        List<String> requiredEntities,
        List<String> missingEntities,
        ProcessingDisposition disposition
) {
    public IntentNormalizationResult {
        Objects.requireNonNull(normalizedRequest, "normalizedRequest must not be null");
        Objects.requireNonNull(primaryQuestion, "primaryQuestion must not be null");
        secondaryQuestions = List.copyOf(secondaryQuestions);
        sceneCandidates = List.copyOf(sceneCandidates);
        subIntentCandidates = List.copyOf(subIntentCandidates);
        requiredEntities = List.copyOf(requiredEntities);
        missingEntities = List.copyOf(missingEntities);
        Objects.requireNonNull(disposition, "disposition must not be null");
    }
}
