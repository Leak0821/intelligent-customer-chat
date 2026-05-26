package com.leak.intelligentcustomerchat.infrastructure.config.runtime;

import com.leak.intelligentcustomerchat.app.config.AgentRuntimeConfigService;
import com.leak.intelligentcustomerchat.app.config.IntentConfigService;
import com.leak.intelligentcustomerchat.app.config.PromptConfigService;
import com.leak.intelligentcustomerchat.app.config.RetrievalConfigService;
import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class CompositeAgentRuntimeConfigService implements AgentRuntimeConfigService, PromptConfigService, IntentConfigService, RetrievalConfigService {
    private final AgentRuntimeConfigLoader localAgentRuntimeConfigLoader;
    private final ObjectProvider<NacosAgentRuntimeConfigLoader> nacosAgentRuntimeConfigLoader;
    private final AtomicReference<AgentRuntimeConfigSnapshot> cache = new AtomicReference<>();

    public CompositeAgentRuntimeConfigService(LocalAgentRuntimeConfigLoader localAgentRuntimeConfigLoader,
                                              ObjectProvider<NacosAgentRuntimeConfigLoader> nacosAgentRuntimeConfigLoader) {
        this.localAgentRuntimeConfigLoader = localAgentRuntimeConfigLoader;
        this.nacosAgentRuntimeConfigLoader = nacosAgentRuntimeConfigLoader;
    }

    @Override
    public AgentRuntimeConfigSnapshot current() {
        AgentRuntimeConfigSnapshot snapshot = cache.get();
        if (snapshot != null) {
            return snapshot;
        }
        return refresh();
    }

    @Override
    public AgentRuntimeConfigSnapshot refresh() {
        AgentRuntimeConfigSnapshot snapshot = loadWithFallback();
        cache.set(snapshot);
        return snapshot;
    }

    @Override
    public PromptTemplateConfig currentPromptConfig() {
        return current().promptTemplateConfig();
    }

    @Override
    public IntentCatalogConfig currentIntentCatalog() {
        return current().intentCatalogConfig();
    }

    @Override
    public RetrievalSettingsConfig currentRetrievalSettings() {
        return current().retrievalSettingsConfig();
    }

    private AgentRuntimeConfigSnapshot loadWithFallback() {
        NacosAgentRuntimeConfigLoader nacosLoader = nacosAgentRuntimeConfigLoader.getIfAvailable();
        if (nacosLoader != null) {
            try {
                return nacosLoader.load();
            } catch (RuntimeException ignored) {
                // Nacos 不可用时回落到本地默认配置，保证应用主链路不被配置中心阻塞。
            }
        }
        return localAgentRuntimeConfigLoader.load();
    }
}
