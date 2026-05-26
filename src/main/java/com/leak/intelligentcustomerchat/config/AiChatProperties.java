package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.chat")
public record AiChatProperties(
        boolean enabled,
        @Min(1) int maxInputLength,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature
) {
}
