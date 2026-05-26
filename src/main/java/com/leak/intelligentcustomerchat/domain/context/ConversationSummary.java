package com.leak.intelligentcustomerchat.domain.context;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public final class ConversationSummary {
    private final String summaryId;
    private final String threadId;
    private final String summaryText;
    private final String summarySource;
    private final int coveredMessageCount;
    private final OffsetDateTime createdAt;

    private ConversationSummary(String summaryId,
                                String threadId,
                                String summaryText,
                                String summarySource,
                                int coveredMessageCount,
                                OffsetDateTime createdAt) {
        this.summaryId = Objects.requireNonNull(summaryId, "summaryId must not be null");
        this.threadId = Objects.requireNonNull(threadId, "threadId must not be null");
        this.summaryText = Objects.requireNonNull(summaryText, "summaryText must not be null");
        this.summarySource = Objects.requireNonNull(summarySource, "summarySource must not be null");
        this.coveredMessageCount = coveredMessageCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ConversationSummary create(String threadId, String summaryText, String summarySource, int coveredMessageCount) {
        return new ConversationSummary(UUID.randomUUID().toString(), threadId, summaryText, summarySource, coveredMessageCount, OffsetDateTime.now());
    }

    public static ConversationSummary restore(String summaryId,
                                              String threadId,
                                              String summaryText,
                                              String summarySource,
                                              int coveredMessageCount,
                                              OffsetDateTime createdAt) {
        return new ConversationSummary(summaryId, threadId, summaryText, summarySource, coveredMessageCount, createdAt);
    }

    public String getSummaryId() {
        return summaryId;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getSummarySource() {
        return summarySource;
    }

    public int getCoveredMessageCount() {
        return coveredMessageCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
