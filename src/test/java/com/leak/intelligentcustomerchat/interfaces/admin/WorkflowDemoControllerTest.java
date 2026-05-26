package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.reply.ReplyDispatchCompensationService;
import com.leak.intelligentcustomerchat.app.reply.ReplySendLifecycleService;
import com.leak.intelligentcustomerchat.app.review.ReplyDraftRevisionService;
import com.leak.intelligentcustomerchat.app.review.ReplyReviewLifecycleService;
import com.leak.intelligentcustomerchat.app.workflow.DemoScenarioCatalogService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowAnalysisService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowQueueAdminService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowQueueItemView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowDemoControllerTest {

    @Test
    void shouldExposeReviewAndDispatchQueues() {
        DemoScenarioCatalogService demoScenarioCatalogService = mock(DemoScenarioCatalogService.class);
        WorkflowQueueAdminService workflowQueueAdminService = mock(WorkflowQueueAdminService.class);
        MailIngestionService mailIngestionService = mock(MailIngestionService.class);
        WorkflowRunService workflowRunService = mock(WorkflowRunService.class);
        WorkflowAnalysisService workflowAnalysisService = mock(WorkflowAnalysisService.class);
        WorkflowEvaluationService workflowEvaluationService = mock(WorkflowEvaluationService.class);
        ReplySendLifecycleService replySendLifecycleService = mock(ReplySendLifecycleService.class);
        ReplyDispatchCompensationService replyDispatchCompensationService = mock(ReplyDispatchCompensationService.class);
        ReplyReviewLifecycleService replyReviewLifecycleService = mock(ReplyReviewLifecycleService.class);
        ReplyDraftRevisionService replyDraftRevisionService = mock(ReplyDraftRevisionService.class);

        WorkflowQueueItemView reviewItem = new WorkflowQueueItemView(
                "run-review", "msg-review", "thread-review", "review@example.com", "Need review",
                "COMPLETED", "COMPLETED", "DRAFT_READY", "PENDING_REVIEW", "await_review_decision",
                null, null, null, "RESUBMIT_REVIEW", "editor-a", "resubmitted", OffsetDateTime.now(), OffsetDateTime.now()
        );
        WorkflowQueueItemView dispatchItem = new WorkflowQueueItemView(
                "run-dispatch", "msg-dispatch", "thread-dispatch", "dispatch@example.com", "Ready to dispatch",
                "COMPLETED", "COMPLETED", "DRAFT_READY", "READY_FOR_SEND", "dispatch_reply",
                "PENDING", 0, null, "APPROVE_SEND", "reviewer-a", "approved", OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(workflowQueueAdminService.listReviewQueue(10)).thenReturn(List.of(reviewItem));
        when(workflowQueueAdminService.listDispatchQueue(10)).thenReturn(List.of(dispatchItem));

        WorkflowDemoController controller = new WorkflowDemoController(
                demoScenarioCatalogService,
                workflowQueueAdminService,
                mailIngestionService,
                workflowRunService,
                workflowAnalysisService,
                workflowEvaluationService,
                replySendLifecycleService,
                replyDispatchCompensationService,
                replyReviewLifecycleService,
                replyDraftRevisionService
        );

        assertThat(controller.listReviewQueue(10)).containsExactly(reviewItem);
        assertThat(controller.listDispatchQueue(10)).containsExactly(dispatchItem);
    }
}
