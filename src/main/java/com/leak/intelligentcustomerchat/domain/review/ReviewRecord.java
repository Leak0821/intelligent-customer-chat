package com.leak.intelligentcustomerchat.domain.review;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class ReviewRecord {
    private final String reviewId;
    private final String runId;
    private final String draftId;
    private final ReviewAction action;
    private final String reviewer;
    private final String reviewNote;
    private final OffsetDateTime createdAt;

    private ReviewRecord(String reviewId,
                         String runId,
                         String draftId,
                         ReviewAction action,
                         String reviewer,
                         String reviewNote,
                         OffsetDateTime createdAt) {
        this.reviewId = Objects.requireNonNull(reviewId, "reviewId must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.reviewer = normalizeReviewer(reviewer);
        this.reviewNote = normalizeReviewNote(reviewNote);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ReviewRecord approveSend(String runId, String draftId, String reviewer, String reviewNote) {
        return new ReviewRecord(UUID.randomUUID().toString(), runId, draftId, ReviewAction.APPROVE_SEND, reviewer, reviewNote, OffsetDateTime.now());
    }

    public static ReviewRecord rejectSend(String runId, String draftId, String reviewer, String reviewNote) {
        return new ReviewRecord(UUID.randomUUID().toString(), runId, draftId, ReviewAction.REJECT_SEND, reviewer, reviewNote, OffsetDateTime.now());
    }

    public static ReviewRecord restore(String reviewId,
                                       String runId,
                                       String draftId,
                                       ReviewAction action,
                                       String reviewer,
                                       String reviewNote,
                                       OffsetDateTime createdAt) {
        return new ReviewRecord(reviewId, runId, draftId, action, reviewer, reviewNote, createdAt);
    }

    public String getReviewId() {
        return reviewId;
    }

    public String getRunId() {
        return runId;
    }

    public String getDraftId() {
        return draftId;
    }

    public ReviewAction getAction() {
        return action;
    }

    public String getReviewer() {
        return reviewer;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isApproval() {
        return action == ReviewAction.APPROVE_SEND;
    }

    private static String normalizeReviewer(String reviewer) {
        if (reviewer == null || reviewer.isBlank()) {
            return "system";
        }
        return reviewer.trim();
    }

    private static String normalizeReviewNote(String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            return "no review note";
        }
        return reviewNote.trim();
    }
}
