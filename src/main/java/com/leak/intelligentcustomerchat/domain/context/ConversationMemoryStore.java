package com.leak.intelligentcustomerchat.domain.context;

public interface ConversationMemoryStore {
    ContextSnapshot read(String threadId);

    void appendCustomerMessage(String threadId, String message);
}
