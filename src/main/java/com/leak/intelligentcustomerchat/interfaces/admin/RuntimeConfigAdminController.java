package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.config.AgentRuntimeConfigService;
import com.leak.intelligentcustomerchat.app.config.RuntimePreflightService;
import com.leak.intelligentcustomerchat.app.config.RuntimePreflightStatus;
import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime-config")
public class RuntimeConfigAdminController {
    private final AgentRuntimeConfigService agentRuntimeConfigService;
    private final RuntimePreflightService runtimePreflightService;

    public RuntimeConfigAdminController(AgentRuntimeConfigService agentRuntimeConfigService,
                                        RuntimePreflightService runtimePreflightService) {
        this.agentRuntimeConfigService = agentRuntimeConfigService;
        this.runtimePreflightService = runtimePreflightService;
    }

    @GetMapping
    public AgentRuntimeConfigSnapshot current() {
        return agentRuntimeConfigService.current();
    }

    @PostMapping("/refresh")
    public AgentRuntimeConfigSnapshot refresh() {
        return agentRuntimeConfigService.refresh();
    }

    @GetMapping("/preflight")
    public RuntimePreflightStatus preflight() {
        return runtimePreflightService.inspect();
    }
}
