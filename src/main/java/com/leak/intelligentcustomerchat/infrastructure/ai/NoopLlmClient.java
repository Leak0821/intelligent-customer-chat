package com.leak.intelligentcustomerchat.infrastructure.ai;

import com.leak.intelligentcustomerchat.app.ai.LlmClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoopLlmClient implements LlmClient {
    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        return Optional.empty();
    }
}
