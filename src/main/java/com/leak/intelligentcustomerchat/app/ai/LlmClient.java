package com.leak.intelligentcustomerchat.app.ai;

import java.util.Optional;

public interface LlmClient {
    Optional<String> complete(String systemPrompt, String userPrompt);
}
