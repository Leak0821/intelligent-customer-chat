package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @NotBlank String source,
        @Min(1) int pollSize,
        String host,
        @Min(1) int port,
        String username,
        String password,
        String folder,
        boolean sslEnabled
) {
}
