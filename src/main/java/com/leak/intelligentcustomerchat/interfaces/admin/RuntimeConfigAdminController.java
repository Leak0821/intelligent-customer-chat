package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.config.AgentRuntimeConfigService;
import com.leak.intelligentcustomerchat.app.config.PromptConfigService;
import com.leak.intelligentcustomerchat.app.config.RuntimePreflightService;
import com.leak.intelligentcustomerchat.app.config.RuntimePreflightStatus;
import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime-config")
public class RuntimeConfigAdminController {
    private final AgentRuntimeConfigService agentRuntimeConfigService;
    private final PromptConfigService promptConfigService;
    private final RuntimePreflightService runtimePreflightService;

    public RuntimeConfigAdminController(AgentRuntimeConfigService agentRuntimeConfigService,
                                        PromptConfigService promptConfigService,
                                        RuntimePreflightService runtimePreflightService) {
        this.agentRuntimeConfigService = agentRuntimeConfigService;
        this.promptConfigService = promptConfigService;
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

    @GetMapping("/prompts/preview")
    public RuntimePromptPreviewView previewPrompt(@RequestParam(defaultValue = "AFTER_SALES") String scene,
                                                  @RequestParam(defaultValue = "Please help verify the latest customer request.") String primaryQuestion) {
        AgentRuntimeConfigSnapshot snapshot = agentRuntimeConfigService.current();
        PromptTemplateConfig promptConfig = promptConfigService.currentPromptConfig();
        String normalizedScene = scene == null || scene.isBlank() ? "AFTER_SALES" : scene.trim().toUpperCase(java.util.Locale.ROOT);
        return new RuntimePromptPreviewView(
                snapshot.source(),
                normalizedScene,
                primaryQuestion,
                renderTemplate(promptConfig.followUpTemplateForScene(normalizedScene), primaryQuestion, normalizedScene),
                renderTemplate(promptConfig.humanReviewTemplateForScene(normalizedScene), primaryQuestion, normalizedScene),
                promptConfig.directReplySuffixForScene(normalizedScene),
                promptConfig.sceneTemplateConfig().followUpTemplatesByScene().keySet().stream().sorted().toList(),
                promptConfig.sceneTemplateConfig().humanReviewTemplatesByScene().keySet().stream().sorted().toList(),
                promptConfig.sceneTemplateConfig().directReplySuffixByScene().keySet().stream().sorted().toList()
        );
    }

    @GetMapping("/preflight")
    public RuntimePreflightStatus preflight() {
        return runtimePreflightService.inspect();
    }

    private String renderTemplate(String template, String primaryQuestion, String scene) {
        return template
                .replace("{{primaryQuestion}}", primaryQuestion)
                .replace("{{scene}}", scene);
    }
}
