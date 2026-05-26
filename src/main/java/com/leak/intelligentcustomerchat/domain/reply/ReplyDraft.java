package com.leak.intelligentcustomerchat.domain.reply;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class ReplyDraft {
    private final String draftId;
    private final String runId;
    private String subject;
    private String body;
    private ReplyDraftStatus status;
    private String reviewNotes;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private ReplyDraft(String draftId,
                       String runId,
                       String subject,
                       String body,
                       ReplyDraftStatus status,
                       String reviewNotes,
                       OffsetDateTime createdAt,
                       OffsetDateTime updatedAt) {
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reviewNotes = Objects.requireNonNull(reviewNotes, "reviewNotes must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ReplyDraft create(String runId, String subject, String body, ReplyDraftStatus status, String reviewNotes) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReplyDraft(UUID.randomUUID().toString(), runId, subject, body, status, reviewNotes, now, now);
    }

    public static ReplyDraft restore(String draftId,
                                     String runId,
                                     String subject,
                                     String body,
                                     ReplyDraftStatus status,
                                     String reviewNotes,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime updatedAt) {
        return new ReplyDraft(draftId, runId, subject, body, status, reviewNotes, createdAt, updatedAt);
    }

    public void revise(String subject, String body, ReplyDraftStatus status, String reviewNotes) {
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reviewNotes = Objects.requireNonNull(reviewNotes, "reviewNotes must not be null");
        this.updatedAt = OffsetDateTime.now();
    }

    public String getDraftId() {
        return draftId;
    }

    public String getRunId() {
        return runId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public ReplyDraftStatus getStatus() {
        return status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
