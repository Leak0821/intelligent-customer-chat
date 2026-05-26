package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail.outbound")
public record MailOutboundProperties(
        boolean enabled,
        @Email String fromAddress,
        @NotBlank String fromName
) {
}
