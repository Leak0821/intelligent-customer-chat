package com.leak.intelligentcustomerchat.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AgentRuntimeConfigServiceTest {
    @Autowired
    private AgentRuntimeConfigService agentRuntimeConfigService;

    @Autowired
    private PromptConfigService promptConfigService;

    @Autowired
    private IntentConfigService intentConfigService;

    @Autowired
    private RetrievalConfigService retrievalConfigService;

    @Test
    void shouldFallbackToLocalRuntimeConfigWhenNacosIsDisabled() {
        assertThat(agentRuntimeConfigService.current().source()).isEqualTo("local-default");
        assertThat(promptConfigService.currentPromptConfig().followUpTemplate()).contains("please share your order number");
        assertThat(intentConfigService.currentIntentCatalog().afterSalesIntents()).contains("logistics_tracking");
        assertThat(retrievalConfigService.currentRetrievalSettings().topK()).isEqualTo(10);
    }
}
