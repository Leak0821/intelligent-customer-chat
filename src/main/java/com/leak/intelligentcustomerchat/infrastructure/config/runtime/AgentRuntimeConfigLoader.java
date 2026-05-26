package com.leak.intelligentcustomerchat.infrastructure.config.runtime;

import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;

public interface AgentRuntimeConfigLoader {
    AgentRuntimeConfigSnapshot load();
}
