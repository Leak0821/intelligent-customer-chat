package com.leak.intelligentcustomerchat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.runtime-config.nacos")
public record NacosRuntimeConfigProperties(
        boolean enabled,
        @NotBlank String serverAddr,
        String namespace,
        @NotBlank String group,
        @NotBlank String promptDataId,
        @NotBlank String intentDataId,
        @NotBlank String retrievalDataId,
        @Min(1000) long timeoutMillis
) {
}
