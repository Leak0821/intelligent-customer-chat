package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.review.ReplyDraftRevisionService;
import com.leak.intelligentcustomerchat.app.review.ReplyReviewLifecycleService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class DemoReviewLoopService {
    private static final String FIRST_REVIEWER = "demo-auditor-1";
    private static final String EDITOR = "demo-editor-1";
    private static final String FINAL_REVIEWER = "demo-auditor-2";
    private static final String REJECT_NOTE = "remove unsupported refund or compensation promise before manual approval";
    private static final String RESUBMIT_NOTE = "revised to keep policy-safe wording and resubmitted for approval";
    private static final String APPROVE_NOTE = "revised draft verified and ready for manual send review";

    private final MailIngestionService mailIngestionService;
    private final WorkflowRunService workflowRunService;
    private final WorkflowEvaluationService workflowEvaluationService;
    private final ReplyReviewLifecycleService replyReviewLifecycleService;
    private final ReplyDraftRevisionService replyDraftRevisionService;

    public DemoReviewLoopService(MailIngestionService mailIngestionService,
                                 WorkflowRunService workflowRunService,
                                 WorkflowEvaluationService workflowEvaluationService,
                                 ReplyReviewLifecycleService replyReviewLifecycleService,
                                 ReplyDraftRevisionService replyDraftRevisionService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
        this.workflowEvaluationService = workflowEvaluationService;
        this.replyReviewLifecycleService = replyReviewLifecycleService;
        this.replyDraftRevisionService = replyDraftRevisionService;
    }

    public DemoReviewLoopExecutionView execute(InboundMail inboundMail) {
        WorkflowRun run = mailIngestionService.process(inboundMail);
        ReplyDraft initialDraft = requireDraft(run.getRunId());
        if (initialDraft.getStatus() != ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            throw new IllegalStateException("review_loop mode requires a HUMAN_REVIEW_REQUIRED draft, but got " + initialDraft.getStatus());
        }
        DemoReviewLoopDraftView initialDraftView = DemoReviewLoopDraftView.from(initialDraft);

        ReplyDraft rejectedDraft = replyReviewLifecycleService.rejectSend(run.getRunId(), FIRST_REVIEWER, REJECT_NOTE);
        DemoReviewLoopDraftView rejectedDraftView = DemoReviewLoopDraftView.from(rejectedDraft);
        ReplyDraft resubmittedDraft = replyDraftRevisionService.revise(
                run.getRunId(),
                EDITOR,
                rejectedDraft.getSubject(),
                buildSafeManualReviewBody(),
                RESUBMIT_NOTE,
                true
        );
        DemoReviewLoopDraftView resubmittedDraftView = DemoReviewLoopDraftView.from(resubmittedDraft);
        ReplyDraft approvedDraft = replyReviewLifecycleService.approveForSend(run.getRunId(), FINAL_REVIEWER, APPROVE_NOTE);

        return new DemoReviewLoopExecutionView(
                run,
                initialDraftView,
                rejectedDraftView,
                resubmittedDraftView,
                DemoReviewLoopDraftView.from(approvedDraft),
                workflowRunService.findReplay(run.getRunId())
                        .orElseThrow(() -> new NoSuchElementException("workflow replay not found for runId=" + run.getRunId())),
                workflowEvaluationService.getSample(run.getRunId()),
                replyReviewLifecycleService.findReviews(run.getRunId())
        );
    }

    private ReplyDraft requireDraft(String runId) {
        return workflowRunService.findDraft(runId)
                .orElseThrow(() -> new NoSuchElementException("draft not found for runId=" + runId));
    }

    private String buildSafeManualReviewBody() {
        return """
                Hello,

                We reviewed your delayed order request and checked the currently available order and logistics context.

                At this stage, we are not confirming refund or compensation until the case is manually verified against the latest order facts and standard after-sales policy.

                We can continue helping with the latest shipment status and the next supported handling step once the manual review is complete.

                Best regards,
                Support Team
                """;
    }
}
