package com.leak.intelligentcustomerchat.domain.context;

import java.util.Optional;

public interface ConversationSummaryRepository {
    ConversationSummary save(ConversationSummary summary);

    Optional<ConversationSummary> findLatestByThreadId(String threadId);
}
