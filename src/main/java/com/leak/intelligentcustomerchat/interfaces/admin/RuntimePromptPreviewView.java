package com.leak.intelligentcustomerchat.interfaces.admin;

import java.util.List;
import java.util.Objects;

public record RuntimePromptPreviewView(
        String source,
        String scene,
        String primaryQuestion,
        String followUpTemplate,
        String humanReviewTemplate,
        String directReplySuffix,
        List<String> availableFollowUpScenes,
        List<String> availableHumanReviewScenes,
        List<String> availableDirectReplySuffixScenes
) {
    public RuntimePromptPreviewView {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(primaryQuestion, "primaryQuestion must not be null");
        Objects.requireNonNull(followUpTemplate, "followUpTemplate must not be null");
        Objects.requireNonNull(humanReviewTemplate, "humanReviewTemplate must not be null");
        Objects.requireNonNull(directReplySuffix, "directReplySuffix must not be null");
        Objects.requireNonNull(availableFollowUpScenes, "availableFollowUpScenes must not be null");
        Objects.requireNonNull(availableHumanReviewScenes, "availableHumanReviewScenes must not be null");
        Objects.requireNonNull(availableDirectReplySuffixScenes, "availableDirectReplySuffixScenes must not be null");
        availableFollowUpScenes = List.copyOf(availableFollowUpScenes);
        availableHumanReviewScenes = List.copyOf(availableHumanReviewScenes);
        availableDirectReplySuffixScenes = List.copyOf(availableDirectReplySuffixScenes);
    }
}
