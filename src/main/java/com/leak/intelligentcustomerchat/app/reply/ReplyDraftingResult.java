package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;

import java.util.Objects;

public record ReplyDraftingResult(
        ReplyDraft draft,
        ReplyDraftingDiagnostics diagnostics
) {
    public ReplyDraftingResult {
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
    }
}
