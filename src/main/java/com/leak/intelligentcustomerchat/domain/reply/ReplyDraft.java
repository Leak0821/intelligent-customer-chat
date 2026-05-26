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
    private int draftVersion;
    private String lastEditedBy;
    private SendReadiness sendReadiness;
    private String nextAction;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private ReplyDraft(String draftId,
                       String runId,
                       String subject,
                       String body,
                       ReplyDraftStatus status,
                       String reviewNotes,
                       int draftVersion,
                       String lastEditedBy,
                       SendReadiness sendReadiness,
                       String nextAction,
                       OffsetDateTime createdAt,
                       OffsetDateTime updatedAt) {
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reviewNotes = Objects.requireNonNull(reviewNotes, "reviewNotes must not be null");
        this.draftVersion = validateDraftVersion(draftVersion);
        this.lastEditedBy = normalizeActor(lastEditedBy);
        this.sendReadiness = Objects.requireNonNull(sendReadiness, "sendReadiness must not be null");
        this.nextAction = Objects.requireNonNull(nextAction, "nextAction must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ReplyDraft create(String runId, String subject, String body, ReplyDraftStatus status, String reviewNotes) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReplyDraft(
                UUID.randomUUID().toString(),
                runId,
                subject,
                body,
                status,
                reviewNotes,
                1,
                "system",
                deriveInitialSendReadiness(status),
                deriveInitialNextAction(status),
                now,
                now
        );
    }

    public static ReplyDraft restore(String draftId,
                                     String runId,
                                     String subject,
                                     String body,
                                     ReplyDraftStatus status,
                                     String reviewNotes,
                                     int draftVersion,
                                     String lastEditedBy,
                                     SendReadiness sendReadiness,
                                     String nextAction,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime updatedAt) {
        return new ReplyDraft(draftId, runId, subject, body, status, reviewNotes, draftVersion, lastEditedBy, sendReadiness, nextAction, createdAt, updatedAt);
    }

    public void revise(String subject, String body, ReplyDraftStatus status, String reviewNotes) {
        revise(subject, body, status, reviewNotes, this.lastEditedBy);
    }

    public void revise(String subject, String body, ReplyDraftStatus status, String reviewNotes, String editedBy) {
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reviewNotes = Objects.requireNonNull(reviewNotes, "reviewNotes must not be null");
        this.draftVersion += 1;
        this.lastEditedBy = normalizeActor(editedBy);
        this.updatedAt = OffsetDateTime.now();
    }

    public void applyReviewOutcome(ReplyDraftStatus status, String reviewNotes) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reviewNotes = Objects.requireNonNull(reviewNotes, "reviewNotes must not be null");
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateSendReadiness(SendReadiness sendReadiness, String nextAction, String reviewNotes) {
        this.sendReadiness = Objects.requireNonNull(sendReadiness, "sendReadiness must not be null");
        this.nextAction = Objects.requireNonNull(nextAction, "nextAction must not be null");
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

    public boolean isDraftReady() {
        return status == ReplyDraftStatus.DRAFT_READY;
    }

    public boolean isFollowUpNeeded() {
        return status == ReplyDraftStatus.FOLLOW_UP_NEEDED;
    }

    public boolean isHumanReviewRequired() {
        return status == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED;
    }

    public boolean isBlocked() {
        return status == ReplyDraftStatus.BLOCKED;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public int getDraftVersion() {
        return draftVersion;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public SendReadiness getSendReadiness() {
        return sendReadiness;
    }

    public String getNextAction() {
        return nextAction;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static SendReadiness deriveInitialSendReadiness(ReplyDraftStatus status) {
        return switch (status) {
            case DRAFT_READY, HUMAN_REVIEW_REQUIRED -> SendReadiness.PENDING_REVIEW;
            case FOLLOW_UP_NEEDED -> SendReadiness.NOT_APPLICABLE;
            case BLOCKED -> SendReadiness.HOLD;
        };
    }

    private static String deriveInitialNextAction(ReplyDraftStatus status) {
        return switch (status) {
            case DRAFT_READY -> "await_review_decision";
            case FOLLOW_UP_NEEDED -> "request_customer_information";
            case HUMAN_REVIEW_REQUIRED -> "manual_review_required";
            case BLOCKED -> "investigate_blocking_issue";
        };
    }

    private static int validateDraftVersion(int draftVersion) {
        if (draftVersion < 1) {
            throw new IllegalArgumentException("draftVersion must be >= 1");
        }
        return draftVersion;
    }

    private static String normalizeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "system";
        }
        return actor.trim();
    }
}
