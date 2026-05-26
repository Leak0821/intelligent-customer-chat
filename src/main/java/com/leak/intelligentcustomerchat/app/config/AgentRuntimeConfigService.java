package com.leak.intelligentcustomerchat.app.config;

import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;

public interface AgentRuntimeConfigService {
    AgentRuntimeConfigSnapshot current();

    AgentRuntimeConfigSnapshot refresh();
}
