package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveContextCompressionServiceTest {

    @Test
    void shouldReturnHeuristicSummaryWhenThresholdReachedAndNoLlmAvailable() {
        ContextMemoryProperties properties = new ContextMemoryProperties(true, true, false, 5, 3, "icc:test");
        ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> provider = new EmptyObjectProvider<>();
        AdaptiveContextCompressionService service = new AdaptiveContextCompressionService(properties, provider);

        var result = service.compress("thread-1", List.of(
                "customer asks about order status",
                "customer follows up with tracking concern",
                "customer asks whether refund is possible"
        ), 3);

        assertThat(result).isPresent();
        assertThat(result.get().getSummaryText()).contains("customer asks whether refund is possible");
        assertThat(result.get().getCoveredMessageCount()).isEqualTo(3);
    }

    @Test
    void shouldSkipCompressionWhenBelowThreshold() {
        ContextMemoryProperties properties = new ContextMemoryProperties(true, true, false, 5, 4, "icc:test");
        ObjectProvider<org.springframework.ai.chat.client.ChatClient.Builder> provider = new EmptyObjectProvider<>();
        AdaptiveContextCompressionService service = new AdaptiveContextCompressionService(properties, provider);

        var result = service.compress("thread-1", List.of("short message", "another message", "third message"), 3);

        assertThat(result).isEmpty();
    }

    private static final class EmptyObjectProvider<T> implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T getIfAvailable() {
            return null;
        }

        @Override
        public T getIfUnique() {
            return null;
        }

        @Override
        public T getObject() {
            throw new UnsupportedOperationException();
        }
    }
}
