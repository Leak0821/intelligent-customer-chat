package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler.xxl")
public record XxlJobProperties(
        boolean enabled,
        @NotBlank String adminAddresses,
        String accessToken,
        @NotBlank String executorAppName,
        String executorAddress,
        String executorIp,
        @Min(1) int executorPort,
        @NotBlank String logPath,
        @Min(1) int logRetentionDays
) {
}
