package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;

import java.util.List;
import java.util.Objects;

public record ReplyDraftingDiagnostics(
        ReplyDraftStatus draftStatus,
        String replySource,
        boolean llmAttempted,
        boolean llmResponseAccepted,
        String fallbackReason,
        String contextMode,
        String systemPrompt,
        String userPrompt,
        List<String> contextPreview,
        List<String> contextStrongSignals,
        List<String> factPreview,
        List<String> knowledgeSnippetIds,
        List<String> knowledgeSnippetPreview
) {
    public ReplyDraftingDiagnostics {
        Objects.requireNonNull(draftStatus, "draftStatus must not be null");
        Objects.requireNonNull(replySource, "replySource must not be null");
        Objects.requireNonNull(contextMode, "contextMode must not be null");
        Objects.requireNonNull(contextPreview, "contextPreview must not be null");
        Objects.requireNonNull(contextStrongSignals, "contextStrongSignals must not be null");
        Objects.requireNonNull(factPreview, "factPreview must not be null");
        Objects.requireNonNull(knowledgeSnippetIds, "knowledgeSnippetIds must not be null");
        Objects.requireNonNull(knowledgeSnippetPreview, "knowledgeSnippetPreview must not be null");
        contextPreview = List.copyOf(contextPreview);
        contextStrongSignals = List.copyOf(contextStrongSignals);
        factPreview = List.copyOf(factPreview);
        knowledgeSnippetIds = List.copyOf(knowledgeSnippetIds);
        knowledgeSnippetPreview = List.copyOf(knowledgeSnippetPreview);
    }

    public static ReplyDraftingDiagnostics unknown(ReplyDraft draft) {
        return new ReplyDraftingDiagnostics(
                draft.getStatus(),
                "unknown",
                false,
                false,
                "draft_service_diagnostics_unavailable",
                "unknown",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
