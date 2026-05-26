package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;

import java.util.List;
import java.util.Optional;

public interface ContextCompressionService {
    Optional<ConversationSummary> compress(String threadId, List<String> recentMessages, long totalMessageCount);
}
