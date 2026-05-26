package com.leak.intelligentcustomerchat.domain.runtime;

import java.time.OffsetDateTime;
import java.util.Objects;

public record AgentRuntimeConfigSnapshot(
        PromptTemplateConfig promptTemplateConfig,
        IntentCatalogConfig intentCatalogConfig,
        RetrievalSettingsConfig retrievalSettingsConfig,
        String source,
        OffsetDateTime loadedAt
) {
    public AgentRuntimeConfigSnapshot {
        Objects.requireNonNull(promptTemplateConfig, "promptTemplateConfig must not be null");
        Objects.requireNonNull(intentCatalogConfig, "intentCatalogConfig must not be null");
        Objects.requireNonNull(retrievalSettingsConfig, "retrievalSettingsConfig must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(loadedAt, "loadedAt must not be null");
    }
}
