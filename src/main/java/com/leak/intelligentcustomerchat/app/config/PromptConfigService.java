package com.leak.intelligentcustomerchat.app.config;

import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;

public interface PromptConfigService {
    PromptTemplateConfig currentPromptConfig();
}
