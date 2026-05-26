package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.context.memory")
public record ContextMemoryProperties(
        boolean enabled,
        boolean compressionEnabled,
        boolean llmSummaryEnabled,
        @Min(1) int recentRoundLimit,
        @Min(2) int summaryThreshold,
        @NotBlank String redisKeyPrefix
) {
}
