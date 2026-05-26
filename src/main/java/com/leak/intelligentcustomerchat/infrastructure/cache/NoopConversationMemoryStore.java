package com.leak.intelligentcustomerchat.infrastructure.cache;

import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.context.memory", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopConversationMemoryStore implements ConversationMemoryStore {

    @Override
    public ContextSnapshot read(String threadId) {
        return ContextSnapshot.empty();
    }

    @Override
    public void appendCustomerMessage(String threadId, String message) {
        // 当前默认实现不落真实缓存，保证本地和测试环境可以无 Redis 启动。
    }

    @Override
    public List<String> recentMessages(String threadId) {
        return List.of();
    }

    @Override
    public void saveSummary(String threadId, String summary) {
    }

    @Override
    public long totalMessageCount(String threadId) {
        return 0L;
    }
}
