package com.leak.intelligentcustomerchat.domain.context;

import java.util.List;

public interface ConversationMemoryStore {
    ContextSnapshot read(String threadId);

    void appendCustomerMessage(String threadId, String message);

    List<String> recentMessages(String threadId);

    void saveSummary(String threadId, String summary);

    long totalMessageCount(String threadId);
}
