package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummaryRepository;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultContextLoadingService implements ContextLoadingService, ContextLoadingTraceService {
    private final ConversationMemoryStore conversationMemoryStore;
    private final ConversationSummaryRepository conversationSummaryRepository;
    private final ContextCompressionService contextCompressionService;
    private final ContextMemoryProperties contextMemoryProperties;

    public DefaultContextLoadingService(ConversationMemoryStore conversationMemoryStore,
                                        ConversationSummaryRepository conversationSummaryRepository,
                                        ContextCompressionService contextCompressionService,
                                        ContextMemoryProperties contextMemoryProperties) {
        this.conversationMemoryStore = conversationMemoryStore;
        this.conversationSummaryRepository = conversationSummaryRepository;
        this.contextCompressionService = contextCompressionService;
        this.contextMemoryProperties = contextMemoryProperties;
    }

    @Override
    public ContextSnapshot load(InboundMail mail, IntentRouteResult routeResult) {
        return diagnose(mail, routeResult).snapshot();
    }

    @Override
    public ContextLoadingDiagnostics diagnose(InboundMail mail, IntentRouteResult routeResult) {
        conversationMemoryStore.appendCustomerMessage(mail.threadId(), mail.rawBody());
        CompressionTrace compressionTrace = maybeCompress(mail.threadId());
        ContextSnapshot refreshedSnapshot = conversationMemoryStore.read(mail.threadId());
        SummaryResolution summaryResolution = resolveSummary(mail, routeResult, refreshedSnapshot);
        ContextSnapshot snapshot = new ContextSnapshot(
                summaryResolution.summaryText(),
                refreshedSnapshot.strongSignals(),
                refreshedSnapshot.optionalSignals()
        );
        return new ContextLoadingDiagnostics(
                snapshot,
                compressionTrace.totalMessageCount(),
                compressionTrace.recentMessageCount(),
                compressionTrace.compressionAttempted(),
                compressionTrace.compressionSucceeded(),
                compressionTrace.compressionDecision(),
                compressionTrace.compressionSkipReason(),
                summaryResolution.source(),
                summaryResolution.restoredPersistedSummaryToMemory()
        );
    }

    private CompressionTrace maybeCompress(String threadId) {
        if (!contextMemoryProperties.compressionEnabled()) {
            return new CompressionTrace(0L, 0, false, false, "skipped", "compression_disabled");
        }
        List<String> recentMessages = conversationMemoryStore.recentMessages(threadId);
        long totalMessageCount = conversationMemoryStore.totalMessageCount(threadId);
        Optional<ConversationSummary> latestSummary = conversationSummaryRepository.findLatestByThreadId(threadId);
        // 这里必须对比线程累计消息数，不能只看最近窗口大小；否则达到阈值后窗口被截断，会导致后续新邮件不再触发重压缩。
        if (latestSummary.isPresent() && latestSummary.get().getCoveredMessageCount() >= totalMessageCount) {
            return new CompressionTrace(totalMessageCount, recentMessages.size(), false, false, "skipped", "summary_already_covers_thread");
        }
        Optional<ConversationSummary> summary = contextCompressionService.compress(threadId, recentMessages, totalMessageCount);
        summary.ifPresent(value -> {
            conversationMemoryStore.saveSummary(threadId, value.getSummaryText());
            conversationSummaryRepository.save(value);
        });
        if (summary.isPresent()) {
            return new CompressionTrace(totalMessageCount, recentMessages.size(), true, true, "generated", null);
        }
        return new CompressionTrace(totalMessageCount, recentMessages.size(), true, false, "skipped", "below_summary_threshold");
    }

    private SummaryResolution resolveSummary(InboundMail mail, IntentRouteResult routeResult, ContextSnapshot refreshedSnapshot) {
        if (!refreshedSnapshot.threadSummary().isBlank()) {
            return new SummaryResolution(refreshedSnapshot.threadSummary(), "memory_summary", false);
        }
        return conversationSummaryRepository.findLatestByThreadId(mail.threadId())
                .map(summary -> {
                    // Redis 摘要被淘汰时，优先回退到数据库中的最新摘要，避免上下文突然断层。
                    conversationMemoryStore.saveSummary(mail.threadId(), summary.getSummaryText());
                    return new SummaryResolution(summary.getSummaryText(), "persisted_summary", true);
                })
                .orElseGet(() -> new SummaryResolution(
                        "thread=%s, latestSubject=%s, route=%s".formatted(mail.threadId(), mail.subject(), routeResult.scene()),
                        "synthetic_summary",
                        false
                ));
    }

    private record CompressionTrace(
            long totalMessageCount,
            int recentMessageCount,
            boolean compressionAttempted,
            boolean compressionSucceeded,
            String compressionDecision,
            String compressionSkipReason
    ) {
    }

    private record SummaryResolution(
            String summaryText,
            String source,
            boolean restoredPersistedSummaryToMemory
    ) {
    }
}
