package com.leak.intelligentcustomerchat.infrastructure.config.runtime;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.config.NacosRuntimeConfigProperties;
import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Properties;

@Component
@ConditionalOnProperty(prefix = "app.runtime-config.nacos", name = "enabled", havingValue = "true")
public class NacosAgentRuntimeConfigLoader implements AgentRuntimeConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(NacosAgentRuntimeConfigLoader.class);

    private final NacosRuntimeConfigProperties properties;
    private final ObjectMapper objectMapper;

    public NacosAgentRuntimeConfigLoader(NacosRuntimeConfigProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentRuntimeConfigSnapshot load() {
        try {
            Properties nacosProperties = new Properties();
            nacosProperties.setProperty("serverAddr", properties.serverAddr());
            if (properties.namespace() != null && !properties.namespace().isBlank()) {
                nacosProperties.setProperty("namespace", properties.namespace());
            }
            ConfigService configService = NacosFactory.createConfigService(nacosProperties);

            PromptTemplateConfig promptTemplateConfig = objectMapper.readValue(
                    configService.getConfig(properties.promptDataId(), properties.group(), properties.timeoutMillis()),
                    PromptTemplateConfig.class
            );
            IntentCatalogConfig intentCatalogConfig = objectMapper.readValue(
                    configService.getConfig(properties.intentDataId(), properties.group(), properties.timeoutMillis()),
                    IntentCatalogConfig.class
            );
            RetrievalSettingsConfig retrievalSettingsConfig = objectMapper.readValue(
                    configService.getConfig(properties.retrievalDataId(), properties.group(), properties.timeoutMillis()),
                    RetrievalSettingsConfig.class
            );

            return new AgentRuntimeConfigSnapshot(
                    promptTemplateConfig,
                    intentCatalogConfig,
                    retrievalSettingsConfig,
                    "nacos",
                    OffsetDateTime.now()
            );
        } catch (Exception ex) {
            log.warn("load runtime config from nacos failed: {}", ex.getMessage());
            throw new IllegalStateException("failed to load runtime config from nacos", ex);
        }
    }
}
