package com.leak.intelligentcustomerchat.infrastructure.cache;

import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.context.memory", name = "enabled", havingValue = "true")
public class RedisConversationMemoryStore implements ConversationMemoryStore {
    private final StringRedisTemplate redisTemplate;
    private final ContextMemoryProperties properties;

    public RedisConversationMemoryStore(StringRedisTemplate redisTemplate, ContextMemoryProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public ContextSnapshot read(String threadId) {
        String summary = redisTemplate.opsForValue().get(summaryKey(threadId));
        List<String> recentMessages = redisTemplate.opsForList().range(messagesKey(threadId), 0, properties.recentRoundLimit() - 1L);
        return new ContextSnapshot(summary == null ? "" : summary, recentMessages == null ? List.of() : recentMessages, List.of());
    }

    @Override
    public void appendCustomerMessage(String threadId, String message) {
        String key = messagesKey(threadId);
        redisTemplate.opsForList().leftPush(key, message);
        redisTemplate.opsForList().trim(key, 0, Math.max(properties.recentRoundLimit(), properties.summaryThreshold()) - 1L);
        redisTemplate.opsForValue().increment(messageCountKey(threadId));
    }

    @Override
    public List<String> recentMessages(String threadId) {
        List<String> messages = redisTemplate.opsForList().range(messagesKey(threadId), 0, properties.summaryThreshold() - 1L);
        return messages == null ? List.of() : messages;
    }

    @Override
    public void saveSummary(String threadId, String summary) {
        redisTemplate.opsForValue().set(summaryKey(threadId), summary);
    }

    @Override
    public long totalMessageCount(String threadId) {
        String value = redisTemplate.opsForValue().get(messageCountKey(threadId));
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private String summaryKey(String threadId) {
        return properties.redisKeyPrefix() + ":thread:" + threadId + ":summary";
    }

    private String messagesKey(String threadId) {
        return properties.redisKeyPrefix() + ":thread:" + threadId + ":messages";
    }

    private String messageCountKey(String threadId) {
        return properties.redisKeyPrefix() + ":thread:" + threadId + ":message-count";
    }
}
