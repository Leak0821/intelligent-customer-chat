package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.config.DispatchRetryProperties;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class DispatchRetryPolicy {
    private final DispatchRetryProperties properties;

    public DispatchRetryPolicy(DispatchRetryProperties properties) {
        this.properties = properties;
    }

    public int maxAttempts() {
        return properties.maxAttempts();
    }

    public boolean retryEnabled() {
        return properties.enabled();
    }

    public int batchSize() {
        return properties.batchSize();
    }

    public OffsetDateTime computeNextRetryAt(ReplyDispatch dispatch, OffsetDateTime attemptedAt) {
        if (!properties.enabled()) {
            return null;
        }
        int nextAttemptCount = dispatch.getAttemptCount() + 1;
        if (nextAttemptCount >= dispatch.getMaxAttempts()) {
            return null;
        }
        long delaySeconds = properties.initialDelaySeconds();
        for (int i = 1; i < nextAttemptCount; i++) {
            delaySeconds = Math.min(delaySeconds * (long) properties.backoffMultiplier(), properties.maxDelaySeconds());
        }
        return attemptedAt.plusSeconds(delaySeconds);
    }
}
