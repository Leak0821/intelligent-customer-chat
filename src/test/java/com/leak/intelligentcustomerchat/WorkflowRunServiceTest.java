package com.leak.intelligentcustomerchat;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.context.ConversationMemoryStore;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStatus;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:intelligent_customer_chat_workflow_run;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
class WorkflowRunServiceTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private WorkflowRunService workflowRunService;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @Autowired
    private WorkflowEventRepository workflowEventRepository;

    @Autowired
    private ReplyDraftRepository replyDraftRepository;

    @Test
    void shouldCompleteWorkflowAndGenerateFollowUpDraftForAfterSalesMailWithoutOrderId() {
        InboundMail mail = new InboundMail(
                "msg-1",
                "thread-1",
                "customer@example.com",
                "Need help",
                " Hello there \n\n\n I need help with my order. ",
                OffsetDateTime.now()
        );

        WorkflowRun run = mailIngestionService.process(mail);
        ReplyDraft draft = replyDraftRepository.findByRunId(run.getRunId()).orElseThrow();

        assertThat(run.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(run.getStage()).isEqualTo(WorkflowStage.COMPLETED);
        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.FOLLOW_UP_NEEDED);
        assertThat(draft.getSendReadiness()).isEqualTo(SendReadiness.NOT_APPLICABLE);
        assertThat(draft.getBody()).contains("order number or tracking number");

        // 这里显式校验仓储落库结果，避免测试只证明了内存态流程成功。
        assertThat(workflowRunRepository.findByRunId(run.getRunId())).isPresent();
        assertThat(workflowRunService.findEvents(run.getRunId())).hasSizeGreaterThanOrEqualTo(8);
        assertThat(workflowEventRepository.findByRunId(run.getRunId())).hasSizeGreaterThanOrEqualTo(8);
        assertThat(workflowRunService.findReplay(run.getRunId())).isPresent();
        assertThat(workflowRunService.findReplayByMessageId(mail.messageId())).isPresent();
        WorkflowEvent draftedEvent = workflowRunService.findEvents(run.getRunId()).stream()
                .filter(event -> event.stage() == WorkflowStage.REPLY_DRAFTED)
                .findFirst()
                .orElseThrow();
        assertThat(draftedEvent.summary()).contains("replySource=follow-up-template");
    }

    @Test
    void shouldGenerateDraftReadyForPreSalesMail() {
        InboundMail mail = new InboundMail(
                "msg-2",
                "thread-2",
                "customer@example.com",
                "Need product recommendation",
                "Can you recommend a product for a living room atmosphere setup?",
                OffsetDateTime.now()
        );

        WorkflowRun run = mailIngestionService.process(mail);
        ReplyDraft draft = workflowRunService.findDraft(run.getRunId()).orElseThrow();

        assertThat(run.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(draft.getSendReadiness()).isEqualTo(SendReadiness.PENDING_REVIEW);
        assertThat(draft.getBody()).contains("first recommendation direction");
    }

    @Test
    void shouldBuildReplayViewWithLatestDraftAndOrderedEvents() {
        InboundMail mail = new InboundMail(
                "msg-3",
                "thread-3",
                "customer@example.com",
                "Check tracking",
                "Please check order #AB123456 and tracking number ZX987654",
                OffsetDateTime.now()
        );

        WorkflowRun run = mailIngestionService.process(mail);
        var replay = workflowRunService.findReplay(run.getRunId()).orElseThrow();

        assertThat(replay.run().getRunId()).isEqualTo(run.getRunId());
        assertThat(replay.events()).isNotEmpty();
        assertThat(replay.events().get(0).createdAt()).isBeforeOrEqualTo(replay.events().get(replay.events().size() - 1).createdAt());
        assertThat(replay.latestDraft()).isNotNull();
        assertThat(replay.evidence().businessFactStatus()).isEqualTo("NO_RESULT");
        assertThat(replay.evidence().businessFactRole()).contains("did not return a usable record");
        assertThat(replay.evidence().knowledgeRetrievalSource()).isNotBlank();
        assertThat(replay.evidence().replySource()).isNotBlank();
        assertThat(replay.dispatches()).isEmpty();
        assertThat(replay.reviews()).isEmpty();
    }

    @Test
    void shouldGenerateHumanReviewDraftForHighRiskAfterSalesMail() {
        InboundMail mail = new InboundMail(
                "msg-human-review-1",
                "thread-human-review-1",
                "buyer@example.com",
                "Need refund and compensation",
                "My order number is ABCD1234 and I want a refund and compensation for the shipping delay.",
                OffsetDateTime.now()
        );

        WorkflowRun run = mailIngestionService.process(mail);
        ReplyDraft draft = workflowRunService.findDraft(run.getRunId()).orElseThrow();

        assertThat(run.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(draft.getStatus()).isEqualTo(ReplyDraftStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(draft.getSendReadiness()).isEqualTo(SendReadiness.PENDING_REVIEW);
        assertThat(draft.getBody()).contains("specialist is checking the case");
    }

    @Test
    void shouldReuseHistoricalOrderIdentifierForFollowUpMailInSameThread() {
        InboundMail firstMail = new InboundMail(
                "msg-thread-reuse-1",
                "thread-reuse-1",
                "buyer@example.com",
                "Order status",
                "My order number is EFGH5678 and I want to know when it will ship.",
                OffsetDateTime.now()
        );
        InboundMail secondMail = new InboundMail(
                "msg-thread-reuse-2",
                "thread-reuse-1",
                "buyer@example.com",
                "Order status again",
                "Can you check the latest order status again?",
                OffsetDateTime.now().plusMinutes(1)
        );

        WorkflowRun firstRun = mailIngestionService.process(firstMail);
        WorkflowRun secondRun = mailIngestionService.process(secondMail);
        ReplyDraft secondDraft = workflowRunService.findDraft(secondRun.getRunId()).orElseThrow();
        WorkflowEvent contextLoadedEvent = workflowRunService.findEvents(secondRun.getRunId()).stream()
                .filter(event -> event.stage() == WorkflowStage.CONTEXT_LOADED)
                .findFirst()
                .orElseThrow();

        assertThat(firstRun.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(secondRun.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(secondDraft.getStatus()).isEqualTo(ReplyDraftStatus.DRAFT_READY);
        assertThat(secondDraft.getSendReadiness()).isEqualTo(SendReadiness.PENDING_REVIEW);
        assertThat(secondDraft.getBody()).doesNotContain("order number or tracking number");
        assertThat(contextLoadedEvent.summary()).contains("reuse_context_order_id");
        var replay = workflowRunService.findReplay(secondRun.getRunId()).orElseThrow();
        assertThat(replay.evidence().businessFactStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldBlockWorkflowWhenDemoFaultScenarioIsTriggered() {
        InboundMail mail = new InboundMail(
                "demo-blocked-runtime-001",
                "thread-blocked-1",
                "customer@example.com",
                "Demo blocked workflow scenario",
                "This built-in scenario is used only to demonstrate the blocked workflow path.",
                OffsetDateTime.now()
        );

        WorkflowRun run = mailIngestionService.process(mail);

        assertThat(run.getStatus()).isEqualTo(WorkflowStatus.BLOCKED);
        assertThat(run.getStage()).isEqualTo(WorkflowStage.BLOCKED);
        assertThat(run.getStatusReason()).contains("demo blocked scenario triggered");
        assertThat(workflowRunService.findDraft(run.getRunId())).isEmpty();
        assertThat(workflowRunService.findReplay(run.getRunId())).isPresent();
    }

    @TestConfiguration
    static class TestMemoryConfig {
        @Bean
        @Primary
        ConversationMemoryStore conversationMemoryStore() {
            return new InMemoryConversationMemoryStore();
        }
    }

    private static final class InMemoryConversationMemoryStore implements ConversationMemoryStore {
        private final java.util.Map<String, Deque<String>> messagesByThread = new java.util.HashMap<>();
        private final java.util.Map<String, String> summariesByThread = new java.util.HashMap<>();
        private final java.util.Map<String, Long> messageCountsByThread = new java.util.HashMap<>();

        @Override
        public synchronized ContextSnapshot read(String threadId) {
            List<String> recentMessages = new ArrayList<>(messagesByThread.getOrDefault(threadId, new ArrayDeque<>()));
            return new ContextSnapshot(
                    summariesByThread.getOrDefault(threadId, ""),
                    List.copyOf(recentMessages),
                    List.of()
            );
        }

        @Override
        public synchronized void appendCustomerMessage(String threadId, String message) {
            Deque<String> messages = messagesByThread.computeIfAbsent(threadId, key -> new ArrayDeque<>());
            messages.addFirst(message);
            while (messages.size() > 10) {
                messages.removeLast();
            }
            messageCountsByThread.merge(threadId, 1L, Long::sum);
        }

        @Override
        public synchronized List<String> recentMessages(String threadId) {
            return new ArrayList<>(messagesByThread.getOrDefault(threadId, new ArrayDeque<>()));
        }

        @Override
        public synchronized void saveSummary(String threadId, String summary) {
            summariesByThread.put(threadId, summary);
        }

        @Override
        public synchronized long totalMessageCount(String threadId) {
            return messageCountsByThread.getOrDefault(threadId, 0L);
        }
    }
}
