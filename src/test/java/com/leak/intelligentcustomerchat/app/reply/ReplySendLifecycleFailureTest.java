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
        "spring.datasource.url=jdbc:h2:mem:intelligent_customer_chat_send_failure;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("test")
class ReplySendLifecycleFailureTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private ReplySendLifecycleService replySendLifecycleService;

    @Autowired
    private ReplyDraftRepository replyDraftRepository;

    @Autowired
    private ReplyDispatchRepository replyDispatchRepository;

    @MockitoBean
    private OutboundMailSender outboundMailSender;

    @Test
    void shouldPersistFailedDispatchAndKeepDraftReadyForRetry() {
        given(outboundMailSender.send(any())).willReturn(OutboundMailSendResult.failed("smtp timeout"));

        WorkflowRun run = mailIngestionService.process(new InboundMail(
                "msg-send-fail-1",
                "thread-send-fail-1",
                "customer@example.com",
                "Where is my order",
                "Please help check logistics status.",
                OffsetDateTime.now()
        ));

        replySendLifecycleService.approveForSend(run.getRunId(), "approved in failure test");
        var dispatch = replySendLifecycleService.dispatch(run.getRunId());

        assertThat(dispatch.getStatus()).isEqualTo(ReplyDispatchStatus.RETRY_PENDING);
        assertThat(dispatch.getErrorMessage()).contains("smtp timeout");
        assertThat(dispatch.getAttemptCount()).isEqualTo(1);
        assertThat(dispatch.getNextRetryAt()).isNotNull();

        var persistedDraft = replyDraftRepository.findByRunId(run.getRunId()).orElseThrow();
        assertThat(persistedDraft.getSendReadiness()).isEqualTo(SendReadiness.READY_FOR_SEND);
        assertThat(persistedDraft.getNextAction()).isEqualTo("await_dispatch_retry");
        assertThat(replyDispatchRepository.findByRunId(run.getRunId())).hasSize(1);
    }
}
