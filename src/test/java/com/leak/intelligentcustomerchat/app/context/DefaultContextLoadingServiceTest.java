package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummaryRepository;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultContextLoadingServiceTest {

    @Test
    void shouldRefreshSummaryWhenThreadKeepsGrowingAfterCompressionThreshold() {
        ContextMemoryProperties properties = new ContextMemoryProperties(true, true, false, 2, 3, "icc:test");
        InMemoryConversationMemoryStore memoryStore = new InMemoryConversationMemoryStore(properties);
        InMemoryConversationSummaryRepository summaryRepository = new InMemoryConversationSummaryRepository();
        CountingCompressionService compressionService = new CountingCompressionService();
        DefaultContextLoadingService service = new DefaultContextLoadingService(
                memoryStore,
                summaryRepository,
                compressionService,
                properties
        );

        service.load(mail("thread-1", "first"), route());
        service.load(mail("thread-1", "second"), route());
        ContextLoadingDiagnostics thirdDiagnostics = service.diagnose(mail("thread-1", "third"), route());
        ContextLoadingDiagnostics fourthDiagnostics = service.diagnose(mail("thread-1", "fourth"), route());

        assertThat(compressionService.invocationCount()).isEqualTo(2);
        assertThat(summaryRepository.findLatestByThreadId("thread-1")).isPresent();
        assertThat(summaryRepository.findLatestByThreadId("thread-1").get().getCoveredMessageCount()).isEqualTo(4);
        assertThat(memoryStore.read("thread-1").threadSummary()).isEqualTo("summary-2");
        assertThat(thirdDiagnostics.compressionAttempted()).isTrue();
        assertThat(thirdDiagnostics.compressionSucceeded()).isTrue();
        assertThat(thirdDiagnostics.summaryResolutionSource()).isEqualTo("memory_summary");
        assertThat(fourthDiagnostics.compressionDecision()).isEqualTo("generated");
    }

    @Test
    void shouldFallbackToPersistedSummaryWhenCacheSummaryIsMissing() {
        ContextMemoryProperties properties = new ContextMemoryProperties(true, false, false, 2, 3, "icc:test");
        InMemoryConversationMemoryStore memoryStore = new InMemoryConversationMemoryStore(properties);
        InMemoryConversationSummaryRepository summaryRepository = new InMemoryConversationSummaryRepository();
        summaryRepository.save(ConversationSummary.create("thread-2", "persisted-summary", "heuristic", 5));
        DefaultContextLoadingService service = new DefaultContextLoadingService(
                memoryStore,
                summaryRepository,
                new NoopCompressionService(),
                properties
        );

        ContextLoadingDiagnostics diagnostics = service.diagnose(mail("thread-2", "latest"), route());
        ContextSnapshot snapshot = diagnostics.snapshot();

        assertThat(snapshot.threadSummary()).isEqualTo("persisted-summary");
        assertThat(memoryStore.read("thread-2").threadSummary()).isEqualTo("persisted-summary");
        assertThat(diagnostics.summaryResolutionSource()).isEqualTo("persisted_summary");
        assertThat(diagnostics.restoredPersistedSummaryToMemory()).isTrue();
        assertThat(diagnostics.compressionSkipReason()).isEqualTo("compression_disabled");
    }

    private static InboundMail mail(String threadId, String body) {
        return new InboundMail("msg-" + body, threadId, "customer@example.com", "Need help", body, OffsetDateTime.now());
    }

    private static IntentRouteResult route() {
        return new IntentRouteResult(CustomerScene.AFTER_SALES, "logistics_status", ProcessingDisposition.CONTINUE, "test");
    }

    private static final class CountingCompressionService implements ContextCompressionService {
        private int invocationCount;

        @Override
        public Optional<ConversationSummary> compress(String threadId, List<String> recentMessages, long totalMessageCount) {
            if (recentMessages.size() < 3) {
                return Optional.empty();
            }
            invocationCount++;
            return Optional.of(ConversationSummary.create(threadId, "summary-" + invocationCount, "heuristic", Math.toIntExact(totalMessageCount)));
        }

        int invocationCount() {
            return invocationCount;
        }
    }

    private static final class NoopCompressionService implements ContextCompressionService {
        @Override
        public Optional<ConversationSummary> compress(String threadId, List<String> recentMessages, long totalMessageCount) {
            return Optional.empty();
        }
    }

    private static final class InMemoryConversationSummaryRepository implements ConversationSummaryRepository {
        private final List<ConversationSummary> summaries = new ArrayList<>();

        @Override
        public ConversationSummary save(ConversationSummary summary) {
            summaries.add(summary);
            return summary;
        }

        @Override
        public Optional<ConversationSummary> findLatestByThreadId(String threadId) {
            return summaries.stream()
                    .filter(summary -> summary.getThreadId().equals(threadId))
                    .reduce((first, second) -> second);
        }
    }

    private static final class InMemoryConversationMemoryStore implements ConversationMemoryStore {
        private final ContextMemoryProperties properties;
        private final java.util.Map<String, Deque<String>> messagesByThread = new java.util.HashMap<>();
        private final java.util.Map<String, String> summariesByThread = new java.util.HashMap<>();
        private final java.util.Map<String, Long> messageCountsByThread = new java.util.HashMap<>();

        private InMemoryConversationMemoryStore(ContextMemoryProperties properties) {
            this.properties = properties;
        }

        @Override
        public ContextSnapshot read(String threadId) {
            List<String> recentMessages = new ArrayList<>(messagesByThread.getOrDefault(threadId, new ArrayDeque<>()));
            int readLimit = Math.min(recentMessages.size(), properties.recentRoundLimit());
            return new ContextSnapshot(
                    summariesByThread.getOrDefault(threadId, ""),
                    recentMessages.subList(0, readLimit),
                    List.of()
            );
        }

        @Override
        public void appendCustomerMessage(String threadId, String message) {
            Deque<String> messages = messagesByThread.computeIfAbsent(threadId, key -> new ArrayDeque<>());
            messages.addFirst(message);
            int maxWindow = Math.max(properties.recentRoundLimit(), properties.summaryThreshold());
            while (messages.size() > maxWindow) {
                messages.removeLast();
            }
            messageCountsByThread.merge(threadId, 1L, Long::sum);
        }

        @Override
        public List<String> recentMessages(String threadId) {
            List<String> messages = new ArrayList<>(messagesByThread.getOrDefault(threadId, new ArrayDeque<>()));
            int readLimit = Math.min(messages.size(), properties.summaryThreshold());
            return messages.subList(0, readLimit);
        }

        @Override
        public void saveSummary(String threadId, String summary) {
            summariesByThread.put(threadId, summary);
        }

        @Override
        public long totalMessageCount(String threadId) {
            return messageCountsByThread.getOrDefault(threadId, 0L);
        }
    }
}
