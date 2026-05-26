package com.leak.intelligentcustomerchat.infrastructure.bootstrap;

import com.leak.intelligentcustomerchat.app.config.RuntimePreflightService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.runtime-preflight", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RuntimePreflightStartupRunner implements ApplicationRunner {
    private final RuntimePreflightService runtimePreflightService;

    public RuntimePreflightStartupRunner(RuntimePreflightService runtimePreflightService) {
        this.runtimePreflightService = runtimePreflightService;
    }

    @Override
    public void run(ApplicationArguments args) {
        runtimePreflightService.ensureReady();
    }
}
