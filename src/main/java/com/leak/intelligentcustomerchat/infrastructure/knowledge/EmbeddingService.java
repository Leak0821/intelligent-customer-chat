package com.leak.intelligentcustomerchat.infrastructure.knowledge;

public interface EmbeddingService {
    float[] embed(String text);

    int dimensions();
}
