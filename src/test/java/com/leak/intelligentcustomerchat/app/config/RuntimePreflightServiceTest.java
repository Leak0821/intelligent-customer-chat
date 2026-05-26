package com.leak.intelligentcustomerchat.app.config;

import com.leak.intelligentcustomerchat.config.AiChatProperties;
import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.config.KnowledgeEmbeddingProperties;
import com.leak.intelligentcustomerchat.config.KnowledgeElasticsearchProperties;
import com.leak.intelligentcustomerchat.config.MailOutboundProperties;
import com.leak.intelligentcustomerchat.config.MailOutboundProvider;
import com.leak.intelligentcustomerchat.config.MailProperties;
import com.leak.intelligentcustomerchat.config.NacosRuntimeConfigProperties;
import com.leak.intelligentcustomerchat.config.XxlJobProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePreflightServiceTest {

    @Test
    void shouldPassLightweightLocalSetupWhenOptionalIntegrationsAreDisabled() {
        RuntimePreflightService service = new RuntimePreflightService(
                new AiChatProperties(false, 6000, 0.2d),
                new MailProperties(false, "imap", 20, "", 993, "", "", "INBOX", true, false, false, 60000L, 5000, 5000),
                new MailOutboundProperties(false, MailOutboundProvider.NOOP, "support@example.com", "intelligent-customer-chat", "", 587, "", "", true, true, false, 5000, 5000, 5000),
                new ContextMemoryProperties(true, true, false, 5, 10, "icc:memory"),
                new KnowledgeEmbeddingProperties(false, 4000),
                new KnowledgeElasticsearchProperties(true, "http://127.0.0.1:9200", "knowledge_chunks", "title", "content", "content_vector", "parent_id", "doc_type", 10, 50, 16, 60),
                new NacosRuntimeConfigProperties(false, "127.0.0.1:8848", "", "DEFAULT_GROUP", "agent-prompts.json", "agent-intents.json", "agent-retrieval.json", 3000L),
                new XxlJobProperties(false, "http://127.0.0.1:8088/xxl-job-admin", "", "intelligent-customer-chat-executor", "", "", 9999, "/tmp/xxl-job/jobhandler", 30),
                "dummy-key-for-slice-1",
                "jdbc:mysql://127.0.0.1:3306/intelligent_customer_chat",
                "127.0.0.1",
                6379
        );

        RuntimePreflightStatus status = service.inspect();

        assertThat(status.ready()).isTrue();
        assertThat(status.mode()).isEqualTo("integrated-local");
        assertThat(status.errors()).isEmpty();
        assertThat(status.checks()).anySatisfy(check -> assertThat(check.feature()).isEqualTo("database"));
    }

    @Test
    void shouldReportMisconfiguredEnabledFeatures() {
        RuntimePreflightService service = new RuntimePreflightService(
                new AiChatProperties(true, 6000, 0.2d),
                new MailProperties(true, "imap", 20, "", 993, "user", "", "INBOX", true, true, true, 60000L, 5000, 5000),
                new MailOutboundProperties(true, MailOutboundProvider.SMTP, "", "intelligent-customer-chat", "", 587, "", "", true, true, false, 5000, 5000, 5000),
                new ContextMemoryProperties(true, true, false, 5, 10, "icc:memory"),
                new KnowledgeEmbeddingProperties(true, 4000),
                new KnowledgeElasticsearchProperties(true, "", "knowledge_chunks", "title", "content", "content_vector", "parent_id", "doc_type", 10, 50, 16, 60),
                new NacosRuntimeConfigProperties(true, "", "", "DEFAULT_GROUP", "agent-prompts.json", "agent-intents.json", "agent-retrieval.json", 3000L),
                new XxlJobProperties(true, "", "", "intelligent-customer-chat-executor", "", "", 9999, "", 30),
                "dummy-key-for-slice-1",
                "jdbc:mysql://127.0.0.1:3306/intelligent_customer_chat",
                "",
                6379
        );

        RuntimePreflightStatus status = service.inspect();

        assertThat(status.ready()).isFalse();
        assertThat(status.errors()).isNotEmpty();
        assertThat(status.errors()).anyMatch(error -> error.contains("OpenAI"));
        assertThat(status.errors()).anyMatch(error -> error.contains("IMAP"));
        assertThat(status.errors()).anyMatch(error -> error.contains("SMTP"));
        assertThat(status.errors()).anyMatch(error -> error.contains("Redis"));
    }
}
