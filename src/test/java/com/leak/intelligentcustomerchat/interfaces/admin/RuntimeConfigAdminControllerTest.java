package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.config.RuntimePreflightService;
import com.leak.intelligentcustomerchat.app.config.RuntimePreflightStatus;
import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptSceneTemplateConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeConfigAdminControllerTest {

    @Test
    void shouldExposeRuntimePreflightStatus() {
        RuntimePreflightStatus status = new RuntimePreflightStatus("lightweight-local", true, List.of(), List.of(), List.of());
        RuntimeConfigAdminController controller = new RuntimeConfigAdminController(
                new StubAgentRuntimeConfigService(),
                new StubAgentRuntimeConfigService(),
                new StubRuntimePreflightService(status)
        );

        assertThat(controller.preflight()).isEqualTo(status);
    }

    @Test
    void shouldPreviewSceneAwarePromptTemplates() {
        RuntimeConfigAdminController controller = new RuntimeConfigAdminController(
                new StubAgentRuntimeConfigService(),
                new StubAgentRuntimeConfigService(),
                new StubRuntimePreflightService(new RuntimePreflightStatus("lightweight-local", true, List.of(), List.of(), List.of()))
        );

        RuntimePromptPreviewView preview = controller.previewPrompt("PRE_SALES", "What product fits my desk setup?");

        assertThat(preview.source()).isEqualTo("local-default");
        assertThat(preview.scene()).isEqualTo("PRE_SALES");
        assertThat(preview.primaryQuestion()).isEqualTo("What product fits my desk setup?");
        assertThat(preview.followUpTemplate()).contains("preferred style");
        assertThat(preview.followUpTemplate()).contains("What product fits my desk setup?");
        assertThat(preview.humanReviewTemplate()).contains("recommendation request");
        assertThat(preview.directReplySuffix()).contains("recommendation evidence");
        assertThat(preview.availableFollowUpScenes()).contains("AFTER_SALES", "PRE_SALES");
    }

    private static final class StubAgentRuntimeConfigService implements com.leak.intelligentcustomerchat.app.config.AgentRuntimeConfigService,
            com.leak.intelligentcustomerchat.app.config.PromptConfigService {
        @Override
        public AgentRuntimeConfigSnapshot current() {
            return snapshot("local-default");
        }

        @Override
        public AgentRuntimeConfigSnapshot refresh() {
            return snapshot("local-refresh");
        }

        private AgentRuntimeConfigSnapshot snapshot(String source) {
            return new AgentRuntimeConfigSnapshot(
                    new PromptTemplateConfig(
                            "prompt",
                            "direct-reply",
                            "follow up {{primaryQuestion}} {{scene}}",
                            "review {{primaryQuestion}} {{scene}}",
                            "suffix",
                            new PromptSceneTemplateConfig(
                                    java.util.Map.of(
                                            "PRE_SALES", "please share preferred style {{primaryQuestion}} {{scene}}",
                                            "AFTER_SALES", "please share order number {{primaryQuestion}} {{scene}}"
                                    ),
                                    java.util.Map.of(
                                            "PRE_SALES", "specialist reviews recommendation request {{primaryQuestion}} {{scene}}",
                                            "AFTER_SALES", "specialist reviews order request {{primaryQuestion}} {{scene}}"
                                    ),
                                    java.util.Map.of(
                                            "PRE_SALES", "recommendation evidence suffix",
                                            "AFTER_SALES", "after sales evidence suffix"
                                    )
                            )
                    ),
                    new IntentCatalogConfig(List.of("product_recommendation"), List.of("logistics_tracking")),
                    new RetrievalSettingsConfig(10, true, 60),
                    source,
                    OffsetDateTime.now()
            );
        }

        @Override
        public PromptTemplateConfig currentPromptConfig() {
            return current().promptTemplateConfig();
        }
    }

    private static final class StubRuntimePreflightService extends RuntimePreflightService {
        private final RuntimePreflightStatus status;

        private StubRuntimePreflightService(RuntimePreflightStatus status) {
            super(
                    new com.leak.intelligentcustomerchat.config.AiChatProperties(false, 6000, 0.2d),
                    new com.leak.intelligentcustomerchat.config.MailProperties(false, "imap", 20, "", 993, "", "", "INBOX", true, false, false, 60000L, 5000, 5000),
                    new com.leak.intelligentcustomerchat.config.MailOutboundProperties(false, com.leak.intelligentcustomerchat.config.MailOutboundProvider.NOOP, "support@example.com", "intelligent-customer-chat", "", 587, "", "", true, true, false, 5000, 5000, 5000),
                    new com.leak.intelligentcustomerchat.config.ContextMemoryProperties(false, true, false, 5, 10, "icc:memory"),
                    new com.leak.intelligentcustomerchat.config.KnowledgeEmbeddingProperties(false, 4000),
                    new com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties(false, "http://127.0.0.1:9200", "knowledge_chunks", "title", "content", "content_vector", "parent_id", "doc_type", 10, 50, 16, 60),
                    new com.leak.intelligentcustomerchat.config.NacosRuntimeConfigProperties(false, "127.0.0.1:8848", "", "DEFAULT_GROUP", "agent-prompts.json", "agent-intents.json", "agent-retrieval.json", 3000L),
                    new com.leak.intelligentcustomerchat.config.XxlJobProperties(false, "http://127.0.0.1:8088/xxl-job-admin", "", "intelligent-customer-chat-executor", "", "", 9999, "/tmp/xxl-job/jobhandler", 30),
                    "dummy-key-for-slice-1",
                    "jdbc:mysql://127.0.0.1:3306/intelligent_customer_chat",
                    "127.0.0.1",
                    6379
            );
            this.status = status;
        }

        @Override
        public RuntimePreflightStatus inspect() {
            return status;
        }
    }
}
