package com.leak.intelligentcustomerchat.infrastructure.ai;

import com.leak.intelligentcustomerchat.app.ai.LlmClient;
import com.leak.intelligentcustomerchat.config.AiChatProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@ConditionalOnBean(ChatClient.Builder.class)
@ConditionalOnProperty(prefix = "app.ai.chat", name = "enabled", havingValue = "true")
public class SpringAiLlmClient implements LlmClient {
    private final ChatClient.Builder chatClientBuilder;
    private final AiChatProperties properties;

    public SpringAiLlmClient(ChatClient.Builder chatClientBuilder, AiChatProperties properties) {
        this.chatClientBuilder = chatClientBuilder;
        this.properties = properties;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        String normalizedSystemPrompt = truncate(systemPrompt);
        String normalizedUserPrompt = truncate(userPrompt);
        String content = chatClientBuilder.clone()
                .defaultOptions(ChatOptions.builder().temperature(properties.temperature()).build())
                .build()
                .prompt()
                .system(normalizedSystemPrompt)
                .user(normalizedUserPrompt)
                .call()
                .content();
        return Optional.ofNullable(content)
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= properties.maxInputLength()
                ? text
                : text.substring(0, properties.maxInputLength());
    }
}
