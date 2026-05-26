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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuntimePreflightService {
    private final AiChatProperties aiChatProperties;
    private final MailProperties mailProperties;
    private final MailOutboundProperties mailOutboundProperties;
    private final ContextMemoryProperties contextMemoryProperties;
    private final KnowledgeEmbeddingProperties knowledgeEmbeddingProperties;
    private final KnowledgeElasticsearchProperties knowledgeElasticsearchProperties;
    private final NacosRuntimeConfigProperties nacosRuntimeConfigProperties;
    private final XxlJobProperties xxlJobProperties;
    private final String openAiApiKey;
    private final String datasourceUrl;
    private final String redisHost;
    private final int redisPort;

    public RuntimePreflightService(AiChatProperties aiChatProperties,
                                   MailProperties mailProperties,
                                   MailOutboundProperties mailOutboundProperties,
                                   ContextMemoryProperties contextMemoryProperties,
                                   KnowledgeEmbeddingProperties knowledgeEmbeddingProperties,
                                   KnowledgeElasticsearchProperties knowledgeElasticsearchProperties,
                                   NacosRuntimeConfigProperties nacosRuntimeConfigProperties,
                                   XxlJobProperties xxlJobProperties,
                                   @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
                                   @Value("${spring.datasource.url:}") String datasourceUrl,
                                   @Value("${spring.data.redis.host:}") String redisHost,
                                   @Value("${spring.data.redis.port:0}") int redisPort) {
        this.aiChatProperties = aiChatProperties;
        this.mailProperties = mailProperties;
        this.mailOutboundProperties = mailOutboundProperties;
        this.contextMemoryProperties = contextMemoryProperties;
        this.knowledgeEmbeddingProperties = knowledgeEmbeddingProperties;
        this.knowledgeElasticsearchProperties = knowledgeElasticsearchProperties;
        this.nacosRuntimeConfigProperties = nacosRuntimeConfigProperties;
        this.xxlJobProperties = xxlJobProperties;
        this.openAiApiKey = normalize(openAiApiKey);
        this.datasourceUrl = normalize(datasourceUrl);
        this.redisHost = normalize(redisHost);
        this.redisPort = redisPort;
    }

    public RuntimePreflightStatus inspect() {
        List<RuntimePreflightStatus.FeatureCheck> checks = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        addDatabaseCheck(checks, errors);
        addRedisCheck(checks, errors);
        addAiCheck(checks, errors);
        addMailCheck(checks, errors, warnings);
        addOutboundMailCheck(checks, errors, warnings);
        addKnowledgeCheck(checks, errors);
        addNacosCheck(checks, errors);
        addXxlJobCheck(checks, errors);

        String mode = deriveMode();
        return new RuntimePreflightStatus(
                mode,
                errors.isEmpty(),
                List.copyOf(checks),
                List.copyOf(errors),
                List.copyOf(warnings)
        );
    }

    public void ensureReady() {
        RuntimePreflightStatus status = inspect();
        if (status.ready()) {
            return;
        }
        throw new IllegalStateException("Runtime preflight failed: " + String.join("; ", status.errors()));
    }

    private void addDatabaseCheck(List<RuntimePreflightStatus.FeatureCheck> checks, List<String> errors) {
        if (datasourceUrl.isBlank()) {
            errors.add("数据库连接串未配置");
            checks.add(RuntimePreflightStatus.FeatureCheck.error("database", "spring.datasource.url 为空"));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("database", summarizeDatasource(datasourceUrl)));
    }

    private void addRedisCheck(List<RuntimePreflightStatus.FeatureCheck> checks, List<String> errors) {
        if (!contextMemoryProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("redis", "上下文记忆未启用"));
            return;
        }
        if (redisHost.isBlank()) {
            errors.add("Redis 主机未配置");
            checks.add(RuntimePreflightStatus.FeatureCheck.error("redis", "spring.data.redis.host 为空"));
            return;
        }
        if (redisPort <= 0) {
            errors.add("Redis 端口配置无效");
            checks.add(RuntimePreflightStatus.FeatureCheck.error("redis", "spring.data.redis.port 非法"));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("redis", redisHost + ":" + redisPort));
    }

    private void addAiCheck(List<RuntimePreflightStatus.FeatureCheck> checks, List<String> errors) {
        if (!aiChatProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("ai-chat", "聊天模型未启用"));
            return;
        }
        if (isPlaceholderApiKey(openAiApiKey)) {
            errors.add("AI 聊天已启用，但 OpenAI 兼容密钥未配置");
            checks.add(RuntimePreflightStatus.FeatureCheck.error("ai-chat", "spring.ai.openai.api-key 为空或仍是占位值"));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("ai-chat", "chat-model-enabled"));
    }

    private void addMailCheck(List<RuntimePreflightStatus.FeatureCheck> checks,
                              List<String> errors,
                              List<String> warnings) {
        if (!mailProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("mail-inbound", "IMAP 收信未启用"));
            return;
        }
        List<String> missing = new ArrayList<>();
        if (mailProperties.host() == null || mailProperties.host().isBlank()) {
            missing.add("host");
        }
        if (mailProperties.username() == null || mailProperties.username().isBlank()) {
            missing.add("username");
        }
        if (mailProperties.password() == null || mailProperties.password().isBlank()) {
            missing.add("password");
        }
        if (!missing.isEmpty()) {
            errors.add("IMAP 配置不完整: " + String.join(", ", missing));
            checks.add(RuntimePreflightStatus.FeatureCheck.error("mail-inbound", "缺少 " + String.join(", ", missing)));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("mail-inbound", mailProperties.source() + ":" + mailProperties.folder()));

        if (mailProperties.pollingEnabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.ok("mail-polling", "轮询任务已启用"));
        }
        if (!mailProperties.pollingEnabled()) {
            warnings.add("IMAP 收信已启用，但轮询未启用，当前只能通过手工触发或外部调度推进");
        }
    }

    private void addOutboundMailCheck(List<RuntimePreflightStatus.FeatureCheck> checks,
                                      List<String> errors,
                                      List<String> warnings) {
        if (!mailOutboundProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("mail-outbound", "外发邮件未启用"));
            return;
        }
        if (mailOutboundProperties.provider() == MailOutboundProvider.NOOP) {
            warnings.add("外发邮件已启用，但 provider 仍是 NOOP，当前不会真实发信");
            checks.add(RuntimePreflightStatus.FeatureCheck.warn("mail-outbound", "noop provider"));
            return;
        }
        List<String> missing = new ArrayList<>();
        if (mailOutboundProperties.host().isBlank()) {
            missing.add("host");
        }
        if (mailOutboundProperties.fromAddress().isBlank()) {
            missing.add("fromAddress");
        }
        if (mailOutboundProperties.authEnabled() && mailOutboundProperties.username().isBlank()) {
            missing.add("username");
        }
        if (!missing.isEmpty()) {
            errors.add("SMTP 配置不完整: " + String.join(", ", missing));
            checks.add(RuntimePreflightStatus.FeatureCheck.error("mail-outbound", "缺少 " + String.join(", ", missing)));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("mail-outbound", mailOutboundProperties.host()));
    }

    private void addKnowledgeCheck(List<RuntimePreflightStatus.FeatureCheck> checks, List<String> errors) {
        if (!knowledgeElasticsearchProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("knowledge", "ES 混合检索未启用"));
            return;
        }
        List<String> missing = new ArrayList<>();
        if (knowledgeElasticsearchProperties.uris().isBlank()) {
            missing.add("uris");
        }
        if (knowledgeElasticsearchProperties.indexName().isBlank()) {
            missing.add("indexName");
        }
        if (!missing.isEmpty()) {
            errors.add("Elasticsearch 知识检索配置不完整: " + String.join(", ", missing));
            checks.add(RuntimePreflightStatus.FeatureCheck.error("knowledge", "缺少 " + String.join(", ", missing)));
            return;
        }
        if (knowledgeEmbeddingProperties.enabled() && isPlaceholderApiKey(openAiApiKey)) {
            errors.add("向量 embedding 已启用，但 OpenAI 兼容密钥未配置");
            checks.add(RuntimePreflightStatus.FeatureCheck.error("knowledge", "embedding 已启用，但 api-key 仍是占位值"));
            return;
        }
        String embeddingMode = knowledgeEmbeddingProperties.enabled() ? "spring-ai-embedding" : "hashing-embedding";
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("knowledge", knowledgeElasticsearchProperties.uris() + " / " + embeddingMode));
    }

    private void addNacosCheck(List<RuntimePreflightStatus.FeatureCheck> checks, List<String> errors) {
        if (!nacosRuntimeConfigProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("nacos", "运行时配置未启用"));
            return;
        }
        if (nacosRuntimeConfigProperties.serverAddr().isBlank()) {
            errors.add("Nacos serverAddr 未配置");
            checks.add(RuntimePreflightStatus.FeatureCheck.error("nacos", "serverAddr 为空"));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("nacos", nacosRuntimeConfigProperties.serverAddr()));
    }

    private void addXxlJobCheck(List<RuntimePreflightStatus.FeatureCheck> checks, List<String> errors) {
        if (!xxlJobProperties.enabled()) {
            checks.add(RuntimePreflightStatus.FeatureCheck.skipped("xxl-job", "XXL-JOB 未启用"));
            return;
        }
        List<String> missing = new ArrayList<>();
        if (xxlJobProperties.adminAddresses().isBlank()) {
            missing.add("adminAddresses");
        }
        if (xxlJobProperties.executorAppName().isBlank()) {
            missing.add("executorAppName");
        }
        if (xxlJobProperties.logPath().isBlank()) {
            missing.add("logPath");
        }
        if (!missing.isEmpty()) {
            errors.add("XXL-JOB 配置不完整: " + String.join(", ", missing));
            checks.add(RuntimePreflightStatus.FeatureCheck.error("xxl-job", "缺少 " + String.join(", ", missing)));
            return;
        }
        checks.add(RuntimePreflightStatus.FeatureCheck.ok("xxl-job", xxlJobProperties.adminAddresses()));
    }

    private String deriveMode() {
        if (aiChatProperties.enabled()
                || mailProperties.enabled()
                || mailOutboundProperties.enabled()
                || contextMemoryProperties.enabled()
                || knowledgeElasticsearchProperties.enabled()
                || knowledgeEmbeddingProperties.enabled()
                || nacosRuntimeConfigProperties.enabled()
                || xxlJobProperties.enabled()) {
            return "integrated-local";
        }
        return "lightweight-local";
    }

    private String summarizeDatasource(String value) {
        int schemeIndex = value.indexOf("://");
        if (schemeIndex < 0) {
            return value;
        }
        return value.substring(0, schemeIndex + 3) + "***";
    }

    private boolean isPlaceholderApiKey(String value) {
        return value.isBlank() || value.startsWith("dummy-key");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
