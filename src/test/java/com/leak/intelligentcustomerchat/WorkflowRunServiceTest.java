package com.leak.intelligentcustomerchat;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
        assertThat(replay.dispatches()).isEmpty();
        assertThat(replay.reviews()).isEmpty();
    }
}
