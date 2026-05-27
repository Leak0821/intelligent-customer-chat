package com.leak.intelligentcustomerchat.domain.runtime;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record PromptSceneTemplateConfig(
        Map<String, String> followUpTemplatesByScene,
        Map<String, String> humanReviewTemplatesByScene,
        Map<String, String> directReplySuffixByScene
) {
    public PromptSceneTemplateConfig {
        Objects.requireNonNull(followUpTemplatesByScene, "followUpTemplatesByScene must not be null");
        Objects.requireNonNull(humanReviewTemplatesByScene, "humanReviewTemplatesByScene must not be null");
        Objects.requireNonNull(directReplySuffixByScene, "directReplySuffixByScene must not be null");
        followUpTemplatesByScene = normalize(followUpTemplatesByScene);
        humanReviewTemplatesByScene = normalize(humanReviewTemplatesByScene);
        directReplySuffixByScene = normalize(directReplySuffixByScene);
    }

    public static PromptSceneTemplateConfig empty() {
        return new PromptSceneTemplateConfig(Map.of(), Map.of(), Map.of());
    }

    private static Map<String, String> normalize(Map<String, String> input) {
        return java.util.Map.copyOf(input.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().trim().toUpperCase(Locale.ROOT),
                        entry -> entry.getValue().trim(),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                )));
    }
}
