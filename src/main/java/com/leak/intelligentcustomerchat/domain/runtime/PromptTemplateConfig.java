package com.leak.intelligentcustomerchat.domain.runtime;

import java.util.Map;
import java.util.Objects;

public record PromptTemplateConfig(
        String intentNormalizationSystemPrompt,
        String directReplySystemPrompt,
        String followUpTemplate,
        String humanReviewTemplate,
        String directReplySuffix,
        PromptSceneTemplateConfig sceneTemplateConfig
) {
    public PromptTemplateConfig(String intentNormalizationSystemPrompt,
                                String directReplySystemPrompt,
                                String followUpTemplate,
                                String humanReviewTemplate,
                                String directReplySuffix) {
        this(intentNormalizationSystemPrompt,
                directReplySystemPrompt,
                followUpTemplate,
                humanReviewTemplate,
                directReplySuffix,
                PromptSceneTemplateConfig.empty());
    }

    public PromptTemplateConfig {
        Objects.requireNonNull(intentNormalizationSystemPrompt, "intentNormalizationSystemPrompt must not be null");
        Objects.requireNonNull(directReplySystemPrompt, "directReplySystemPrompt must not be null");
        Objects.requireNonNull(followUpTemplate, "followUpTemplate must not be null");
        Objects.requireNonNull(humanReviewTemplate, "humanReviewTemplate must not be null");
        Objects.requireNonNull(directReplySuffix, "directReplySuffix must not be null");
        Objects.requireNonNull(sceneTemplateConfig, "sceneTemplateConfig must not be null");
    }

    public String followUpTemplateForScene(String scene) {
        return resolveSceneOverride(sceneTemplateConfig.followUpTemplatesByScene(), scene, followUpTemplate);
    }

    public String humanReviewTemplateForScene(String scene) {
        return resolveSceneOverride(sceneTemplateConfig.humanReviewTemplatesByScene(), scene, humanReviewTemplate);
    }

    public String directReplySuffixForScene(String scene) {
        return resolveSceneOverride(sceneTemplateConfig.directReplySuffixByScene(), scene, directReplySuffix);
    }

    private String resolveSceneOverride(Map<String, String> overrides, String scene, String fallback) {
        if (scene == null || scene.isBlank()) {
            return overrides.getOrDefault("DEFAULT", fallback);
        }
        String normalizedScene = scene.trim().toUpperCase(java.util.Locale.ROOT);
        if (overrides.containsKey(normalizedScene)) {
            return overrides.get(normalizedScene);
        }
        return overrides.getOrDefault("DEFAULT", fallback);
    }
}
