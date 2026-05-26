package com.leak.intelligentcustomerchat;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkflowRunServiceTest {
    @Autowired
    private MailIngestionService mailIngestionService;

    @Autowired
    private WorkflowRunService workflowRunService;

    @Test
    void shouldCompleteSliceOneWorkflow() {
        InboundMail mail = new InboundMail(
                "msg-1",
                "thread-1",
                "customer@example.com",
                "Need help",
                " Hello there \n\n\n I need help with my order. ",
                OffsetDateTime.now()
        );

        WorkflowRun run = mailIngestionService.process(mail);

        assertThat(run.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(run.getStage()).isEqualTo(WorkflowStage.COMPLETED);

        List<?> events = workflowRunService.findEvents(run.getRunId());
        assertThat(events).hasSizeGreaterThanOrEqualTo(3);
    }
}
