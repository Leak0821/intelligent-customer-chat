package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

@Service
public class DefaultContextLoadingService implements ContextLoadingService {
    private final ConversationMemoryStore conversationMemoryStore;

    public DefaultContextLoadingService(ConversationMemoryStore conversationMemoryStore) {
        this.conversationMemoryStore = conversationMemoryStore;
    }

    @Override
    public ContextSnapshot load(InboundMail mail, IntentRouteResult routeResult) {
        ContextSnapshot memorySnapshot = conversationMemoryStore.read(mail.threadId());
        conversationMemoryStore.appendCustomerMessage(mail.threadId(), mail.rawBody());

        // 当前仍然保留最小聚合逻辑，后续把历史摘要压缩和多轮拼接放到真正的上下文服务里。
        String summary = memorySnapshot.threadSummary().isBlank()
                ? "thread=%s, latestSubject=%s, route=%s".formatted(mail.threadId(), mail.subject(), routeResult.scene())
                : memorySnapshot.threadSummary();
        return new ContextSnapshot(summary, memorySnapshot.strongSignals(), memorySnapshot.optionalSignals());
    }
}
