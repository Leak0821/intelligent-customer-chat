package com.leak.intelligentcustomerchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workflow")
public record WorkflowProperties(
        boolean autoCompleteEmptyChain
) {
}
