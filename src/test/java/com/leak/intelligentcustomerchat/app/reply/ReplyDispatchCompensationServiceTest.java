package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchStatus;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:intelligent_customer_chat_dispatch_retry;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("test")
class ReplyDispatchCompensationServiceTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private ReplySendLifecycleService replySendLifecycleService;

    @Autowired
    private ReplyDispatchCompensationService replyDispatchCompensationService;

    @Autowired
    private ReplyDraftRepository replyDraftRepository;

    @Autowired
    private ReplyDispatchRepository replyDispatchRepository;

    @MockitoBean
    private OutboundMailSender outboundMailSender;

    @Test
    void shouldRetryDueDispatchAndMarkAsSent() {
        given(outboundMailSender.send(any()))
                .willReturn(OutboundMailSendResult.failed("smtp timeout"))
                .willReturn(OutboundMailSendResult.success("smtp-message-2"));

        WorkflowRun run = mailIngestionService.process(new InboundMail(
                "msg-retry-1",
                "thread-retry-1",
                "customer@example.com",
                "Where is my order",
                "Please help check logistics status.",
                OffsetDateTime.now()
        ));

        replySendLifecycleService.approveForSend(run.getRunId(), "approved in retry test");
        var firstDispatch = replySendLifecycleService.dispatch(run.getRunId());
        assertThat(firstDispatch.getStatus()).isEqualTo(ReplyDispatchStatus.RETRY_PENDING);

        var retried = replyDispatchCompensationService.retry(run.getRunId());
        assertThat(retried.getStatus()).isEqualTo(ReplyDispatchStatus.SENT);
        assertThat(retried.getAttemptCount()).isEqualTo(2);
        assertThat(retried.getProviderMessageId()).isEqualTo("smtp-message-2");

        var persistedDraft = replyDraftRepository.findByRunId(run.getRunId()).orElseThrow();
        assertThat(persistedDraft.getSendReadiness()).isEqualTo(SendReadiness.HOLD);
        assertThat(persistedDraft.getNextAction()).isEqualTo("already_dispatched");
        assertThat(replyDispatchRepository.findByRunId(run.getRunId())).hasSize(1);
    }

    @Test
    void shouldMarkDispatchAsFinalFailureWhenRetryBudgetIsExhausted() {
        given(outboundMailSender.send(any())).willReturn(OutboundMailSendResult.failed("smtp timeout"));

        WorkflowRun run = mailIngestionService.process(new InboundMail(
                "msg-retry-2",
                "thread-retry-2",
                "customer@example.com",
                "Where is my order",
                "Please help check logistics status.",
                OffsetDateTime.now()
        ));

        replySendLifecycleService.approveForSend(run.getRunId(), "approved in retry exhaustion test");
        replySendLifecycleService.dispatch(run.getRunId());
        replyDispatchCompensationService.retry(run.getRunId());
        var finalDispatch = replyDispatchCompensationService.retry(run.getRunId());

        assertThat(finalDispatch.getStatus()).isEqualTo(ReplyDispatchStatus.FAILED_FINAL);
        assertThat(finalDispatch.getAttemptCount()).isEqualTo(3);
        assertThat(finalDispatch.getNextRetryAt()).isNull();

        var persistedDraft = replyDraftRepository.findByRunId(run.getRunId()).orElseThrow();
        assertThat(persistedDraft.getSendReadiness()).isEqualTo(SendReadiness.HOLD);
        assertThat(persistedDraft.getNextAction()).isEqualTo("investigate_dispatch_failure");
    }
}
