package com.leak.intelligentcustomerchat.domain.runtime;

import java.util.Objects;

public record PromptTemplateConfig(
        String intentNormalizationSystemPrompt,
        String directReplySystemPrompt,
        String followUpTemplate,
        String humanReviewTemplate,
        String directReplySuffix
) {
    public PromptTemplateConfig {
        Objects.requireNonNull(intentNormalizationSystemPrompt, "intentNormalizationSystemPrompt must not be null");
        Objects.requireNonNull(directReplySystemPrompt, "directReplySystemPrompt must not be null");
        Objects.requireNonNull(followUpTemplate, "followUpTemplate must not be null");
        Objects.requireNonNull(humanReviewTemplate, "humanReviewTemplate must not be null");
        Objects.requireNonNull(directReplySuffix, "directReplySuffix must not be null");
    }
}
