package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdaptiveContextCompressionService implements ContextCompressionService {
    private final ContextMemoryProperties properties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public AdaptiveContextCompressionService(ContextMemoryProperties properties,
                                             ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.properties = properties;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public Optional<ConversationSummary> compress(String threadId, List<String> recentMessages, long totalMessageCount) {
        if (!properties.compressionEnabled() || recentMessages.size() < properties.summaryThreshold()) {
            return Optional.empty();
        }
        List<String> orderedMessages = toChronologicalMessages(recentMessages);

        String summaryText = properties.llmSummaryEnabled()
                ? summarizeByLlmOrFallback(orderedMessages)
                : summarizeHeuristically(orderedMessages);
        return Optional.of(ConversationSummary.create(
                threadId,
                summaryText,
                properties.llmSummaryEnabled() ? "llm-or-heuristic" : "heuristic",
                Math.toIntExact(totalMessageCount)
        ));
    }

    private List<String> toChronologicalMessages(List<String> recentMessages) {
        List<String> orderedMessages = new ArrayList<>(recentMessages);
        // Redis 里是头插，摘要时改成时间正序，便于模型理解整段会话演进。
        java.util.Collections.reverse(orderedMessages);
        return orderedMessages;
    }

    private String summarizeByLlmOrFallback(List<String> recentMessages) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return summarizeHeuristically(recentMessages);
        }
        try {
            String prompt = """
                    Compress the following customer email thread into a concise support summary.
                    Keep key facts, pending asks, explicit order or tracking identifiers, and next-step blockers.
                    
                    Thread:
                    %s
                    """.formatted(String.join("\n", recentMessages));
            String content = builder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            return (content == null || content.isBlank()) ? summarizeHeuristically(recentMessages) : content.trim();
        } catch (RuntimeException ex) {
            return summarizeHeuristically(recentMessages);
        }
    }

    private String summarizeHeuristically(List<String> recentMessages) {
        return recentMessages.stream()
                .limit(3)
                .map(message -> message.length() > 120 ? message.substring(0, 120) + "..." : message)
                .collect(Collectors.joining(" | "));
    }
}
