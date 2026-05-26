package com.leak.intelligentcustomerchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail.dispatch-retry")
public record DispatchRetryProperties(
        boolean enabled,
        int maxAttempts,
        long initialDelaySeconds,
        int backoffMultiplier,
        long maxDelaySeconds,
        int batchSize,
        boolean localSchedulerEnabled,
        long localFixedDelayMillis
) {
    public DispatchRetryProperties {
        maxAttempts = maxAttempts < 1 ? 3 : maxAttempts;
        initialDelaySeconds = initialDelaySeconds < 1 ? 60 : initialDelaySeconds;
        backoffMultiplier = backoffMultiplier < 1 ? 2 : backoffMultiplier;
        maxDelaySeconds = maxDelaySeconds < 1 ? 900 : maxDelaySeconds;
        batchSize = batchSize < 1 ? 20 : batchSize;
        localFixedDelayMillis = localFixedDelayMillis < 1000 ? 60000 : localFixedDelayMillis;
    }
}
