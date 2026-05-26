package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.config.ContextMemoryProperties;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummaryRepository;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

@Service
public class DefaultContextLoadingService implements ContextLoadingService {
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
        conversationMemoryStore.appendCustomerMessage(mail.threadId(), mail.rawBody());
        maybeCompress(mail.threadId());
        ContextSnapshot refreshedSnapshot = conversationMemoryStore.read(mail.threadId());

        String summary = resolveSummary(mail, routeResult, refreshedSnapshot);
        return new ContextSnapshot(summary, refreshedSnapshot.strongSignals(), refreshedSnapshot.optionalSignals());
    }

    private void maybeCompress(String threadId) {
        if (!contextMemoryProperties.compressionEnabled()) {
            return;
        }
        var recentMessages = conversationMemoryStore.recentMessages(threadId);
        long totalMessageCount = conversationMemoryStore.totalMessageCount(threadId);
        var latestSummary = conversationSummaryRepository.findLatestByThreadId(threadId);
        // 这里必须对比线程累计消息数，不能只看最近窗口大小；否则达到阈值后窗口被截断，会导致后续新邮件不再触发重压缩。
        if (latestSummary.isPresent() && latestSummary.get().getCoveredMessageCount() >= totalMessageCount) {
            return;
        }
        contextCompressionService.compress(threadId, recentMessages, totalMessageCount).ifPresent(summary -> {
            conversationMemoryStore.saveSummary(threadId, summary.getSummaryText());
            conversationSummaryRepository.save(summary);
        });
    }

    private String resolveSummary(InboundMail mail, IntentRouteResult routeResult, ContextSnapshot refreshedSnapshot) {
        if (!refreshedSnapshot.threadSummary().isBlank()) {
            return refreshedSnapshot.threadSummary();
        }
        return conversationSummaryRepository.findLatestByThreadId(mail.threadId())
                .map(summary -> {
                    // Redis 摘要被淘汰时，优先回退到数据库中的最新摘要，避免上下文突然断层。
                    conversationMemoryStore.saveSummary(mail.threadId(), summary.getSummaryText());
                    return summary.getSummaryText();
                })
                .orElseGet(() -> "thread=%s, latestSubject=%s, route=%s"
                        .formatted(mail.threadId(), mail.subject(), routeResult.scene()));
    }
}
