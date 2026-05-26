package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchStatus;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReplySendLifecycleServiceTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private ReplySendLifecycleService replySendLifecycleService;

    @Autowired
    private ReplyDraftRepository replyDraftRepository;

    @Autowired
    private ReplyDispatchRepository replyDispatchRepository;

    @Test
    void shouldApproveAndDispatchDraftThroughNoopSender() {
        WorkflowRun run = mailIngestionService.process(new com.leak.intelligentcustomerchat.domain.mail.InboundMail(
                "msg-send-1",
                "thread-send-1",
                "customer@example.com",
                "Need recommendation",
                "Please recommend a product for a living room setup.",
                OffsetDateTime.now()
        ));

        var draftBeforeApproval = replyDraftRepository.findByRunId(run.getRunId()).orElseThrow();
        assertThat(draftBeforeApproval.getSendReadiness()).isEqualTo(SendReadiness.PENDING_REVIEW);

        var approvedDraft = replySendLifecycleService.approveForSend(run.getRunId(), "approved in test");
        assertThat(approvedDraft.getSendReadiness()).isEqualTo(SendReadiness.READY_FOR_SEND);
        assertThat(approvedDraft.getNextAction()).isEqualTo("dispatch_reply");

        var dispatch = replySendLifecycleService.dispatch(run.getRunId());
        assertThat(dispatch.getStatus()).isEqualTo(ReplyDispatchStatus.SENT);
        assertThat(dispatch.getProviderMessageId()).startsWith("noop-");
        assertThat(dispatch.getAttemptCount()).isEqualTo(1);
        assertThat(dispatch.getMaxAttempts()).isEqualTo(3);
        assertThat(dispatch.getNextRetryAt()).isNull();

        var persistedDraft = replyDraftRepository.findByRunId(run.getRunId()).orElseThrow();
        assertThat(persistedDraft.getSendReadiness()).isEqualTo(SendReadiness.HOLD);
        assertThat(persistedDraft.getNextAction()).isEqualTo("already_dispatched");
        assertThat(replyDispatchRepository.findByRunId(run.getRunId())).hasSize(1);
    }
}
