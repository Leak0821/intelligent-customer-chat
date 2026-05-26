package com.leak.intelligentcustomerchat.app.workflow;

import java.time.OffsetDateTime;
import java.util.Objects;

public record WorkflowPersistedSummaryView(
        String summaryId,
        String summarySource,
        int coveredMessageCount,
        OffsetDateTime createdAt,
        String summaryText
) {
    public WorkflowPersistedSummaryView {
        Objects.requireNonNull(summaryId, "summaryId must not be null");
        Objects.requireNonNull(summarySource, "summarySource must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(summaryText, "summaryText must not be null");
    }
}
