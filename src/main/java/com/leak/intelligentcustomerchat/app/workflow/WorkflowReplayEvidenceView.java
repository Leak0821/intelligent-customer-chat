package com.leak.intelligentcustomerchat.app.workflow;

import java.util.List;
import java.util.Objects;

public record WorkflowReplayEvidenceView(
        String businessFactStatus,
        String businessFactRole,
        List<String> businessFactSourceSystems,
        int businessFactCount,
        int businessFactMissingEntityCount,
        int businessFactConflictFlagCount,
        String knowledgeRole,
        String knowledgeRetrievalSource,
        int knowledgeRecallCount,
        List<String> knowledgeSnippetIds,
        String replySource,
        String replyFallbackReason
) {
    public WorkflowReplayEvidenceView {
        Objects.requireNonNull(businessFactStatus, "businessFactStatus must not be null");
        Objects.requireNonNull(businessFactRole, "businessFactRole must not be null");
        Objects.requireNonNull(businessFactSourceSystems, "businessFactSourceSystems must not be null");
        Objects.requireNonNull(knowledgeRole, "knowledgeRole must not be null");
        Objects.requireNonNull(knowledgeRetrievalSource, "knowledgeRetrievalSource must not be null");
        Objects.requireNonNull(knowledgeSnippetIds, "knowledgeSnippetIds must not be null");
        Objects.requireNonNull(replySource, "replySource must not be null");
        businessFactSourceSystems = List.copyOf(businessFactSourceSystems);
        knowledgeSnippetIds = List.copyOf(knowledgeSnippetIds);
    }
}
