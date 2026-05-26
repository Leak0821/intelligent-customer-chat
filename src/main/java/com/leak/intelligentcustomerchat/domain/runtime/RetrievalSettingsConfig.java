package com.leak.intelligentcustomerchat.domain.runtime;

public record RetrievalSettingsConfig(
        int topK,
        boolean factsFirst,
        int rrfWindowSize
) {
}
