package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        boolean enabled,
        String source,
        @Min(1) int pollSize,
        String host,
        @Min(1) int port,
        String username,
        String password,
        String folder,
        boolean sslEnabled,
        boolean markSeenAfterFetch,
        boolean pollingEnabled,
        @Min(1000) long pollIntervalMillis,
        @Min(1000) int connectionTimeoutMillis,
        @Min(1000) int readTimeoutMillis
) {
}
