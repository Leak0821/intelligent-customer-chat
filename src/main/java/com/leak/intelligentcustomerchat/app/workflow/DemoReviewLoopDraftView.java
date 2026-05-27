package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;

import java.util.Objects;

public record DemoReviewLoopDraftView(
        String draftId,
        Integer draftVersion,
        String subject,
        String body,
        String draftStatus,
        String sendReadiness,
        String nextAction,
        String reviewNotes,
        String lastEditedBy
) {
    public DemoReviewLoopDraftView {
        Objects.requireNonNull(draftId, "draftId must not be null");
        Objects.requireNonNull(draftVersion, "draftVersion must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(draftStatus, "draftStatus must not be null");
        Objects.requireNonNull(sendReadiness, "sendReadiness must not be null");
        Objects.requireNonNull(nextAction, "nextAction must not be null");
        Objects.requireNonNull(reviewNotes, "reviewNotes must not be null");
        Objects.requireNonNull(lastEditedBy, "lastEditedBy must not be null");
    }

    public static DemoReviewLoopDraftView from(ReplyDraft draft) {
        return new DemoReviewLoopDraftView(
                draft.getDraftId(),
                draft.getDraftVersion(),
                draft.getSubject(),
                draft.getBody(),
                draft.getStatus().name(),
                draft.getSendReadiness().name(),
                draft.getNextAction(),
                draft.getReviewNotes(),
                draft.getLastEditedBy()
        );
    }
}
