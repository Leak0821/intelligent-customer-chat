package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.reply.DispatchRetryBatchResult;
import com.leak.intelligentcustomerchat.app.reply.ReplyDispatchCompensationService;
import com.leak.intelligentcustomerchat.app.reply.ReplySendLifecycleService;
import com.leak.intelligentcustomerchat.app.review.ReplyReviewLifecycleService;
import com.leak.intelligentcustomerchat.app.review.ReplyDraftRevisionService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowAnalysisService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowAnalysisView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationSampleView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowEvaluationService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowReplayView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/workflows")
public class WorkflowDemoController {
    private final MailIngestionService mailIngestionService;
    private final WorkflowRunService workflowRunService;
    private final WorkflowAnalysisService workflowAnalysisService;
    private final WorkflowEvaluationService workflowEvaluationService;
    private final ReplySendLifecycleService replySendLifecycleService;
    private final ReplyDispatchCompensationService replyDispatchCompensationService;
    private final ReplyReviewLifecycleService replyReviewLifecycleService;
    private final ReplyDraftRevisionService replyDraftRevisionService;

    public WorkflowDemoController(MailIngestionService mailIngestionService,
                                  WorkflowRunService workflowRunService,
                                  WorkflowAnalysisService workflowAnalysisService,
                                  WorkflowEvaluationService workflowEvaluationService,
                                  ReplySendLifecycleService replySendLifecycleService,
                                  ReplyDispatchCompensationService replyDispatchCompensationService,
                                  ReplyReviewLifecycleService replyReviewLifecycleService,
                                  ReplyDraftRevisionService replyDraftRevisionService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
        this.workflowAnalysisService = workflowAnalysisService;
        this.workflowEvaluationService = workflowEvaluationService;
        this.replySendLifecycleService = replySendLifecycleService;
        this.replyDispatchCompensationService = replyDispatchCompensationService;
        this.replyReviewLifecycleService = replyReviewLifecycleService;
        this.replyDraftRevisionService = replyDraftRevisionService;
    }

    @PostMapping("/demo")
    public WorkflowRun triggerDemo(@RequestBody DemoMailRequest request) {
        // 管理口只用于本地验证最小闭环，不代表最终邮件接入协议。
        InboundMail inboundMail = buildInboundMail(request);
        return mailIngestionService.process(inboundMail);
    }

    @PostMapping("/demo/replay")
    public WorkflowReplayView triggerDemoAndReplay(@RequestBody DemoMailRequest request) {
        WorkflowRun run = mailIngestionService.process(buildInboundMail(request));
        return workflowRunService.findReplay(run.getRunId())
                .orElseThrow(() -> new NoSuchElementException("workflow replay not found for runId=" + run.getRunId()));
    }

    @PostMapping("/demo/analysis")
    public WorkflowAnalysisView analyzeDemo(@RequestBody DemoMailRequest request) {
        return workflowAnalysisService.analyze(buildInboundMail(request));
    }

    @GetMapping
    public List<WorkflowRun> listRuns() {
        return workflowRunService.findAllRuns();
    }

    @GetMapping("/{runId}/replay")
    public WorkflowReplayView replay(@PathVariable String runId) {
        return workflowRunService.findReplay(runId)
                .orElseThrow(() -> new NoSuchElementException("workflow replay not found for runId=" + runId));
    }

    @GetMapping("/{runId}/evaluation")
    public WorkflowEvaluationSampleView evaluation(@PathVariable String runId) {
        return workflowEvaluationService.getSample(runId);
    }

    @GetMapping("/evaluations/recent")
    public List<WorkflowEvaluationSampleView> recentEvaluations() {
        return workflowEvaluationService.listRecentSamples(20);
    }

    @GetMapping("/by-message/{messageId}/replay")
    public WorkflowReplayView replayByMessageId(@PathVariable String messageId) {
        return workflowRunService.findReplayByMessageId(messageId)
                .orElseThrow(() -> new NoSuchElementException("workflow replay not found for messageId=" + messageId));
    }

    @GetMapping("/{runId}/events")
    public List<WorkflowEvent> listEvents(@PathVariable String runId) {
        return workflowRunService.findEvents(runId);
    }

    @GetMapping("/{runId}/draft")
    public ReplyDraft getDraft(@PathVariable String runId) {
        return workflowRunService.findDraft(runId)
                .orElseThrow(() -> new NoSuchElementException("draft not found for runId=" + runId));
    }

    @PostMapping("/{runId}/approve-send")
    public ReplyDraft approveSend(@PathVariable String runId, @RequestBody(required = false) SendApprovalRequest request) {
        String reviewer = request == null || request.reviewer() == null || request.reviewer().isBlank()
                ? "demo-admin"
                : request.reviewer();
        String approvalNote = request == null || request.approvalNote() == null || request.approvalNote().isBlank()
                ? "approved for send from demo admin endpoint"
                : request.approvalNote();
        return replyReviewLifecycleService.approveForSend(runId, reviewer, approvalNote);
    }

    @PostMapping("/{runId}/reject-send")
    public ReplyDraft rejectSend(@PathVariable String runId, @RequestBody(required = false) SendRejectRequest request) {
        String reviewer = request == null || request.reviewer() == null || request.reviewer().isBlank()
                ? "demo-admin"
                : request.reviewer();
        String rejectNote = request == null || request.rejectNote() == null || request.rejectNote().isBlank()
                ? "rejected for send from demo admin endpoint"
                : request.rejectNote();
        return replyReviewLifecycleService.rejectSend(runId, reviewer, rejectNote);
    }

    @PostMapping("/{runId}/revise-draft")
    public ReplyDraft reviseDraft(@PathVariable String runId, @RequestBody ReviseDraftRequest request) {
        String editor = request.editor() == null || request.editor().isBlank()
                ? "demo-admin"
                : request.editor();
        return replyDraftRevisionService.revise(
                runId,
                editor,
                request.subject(),
                request.body(),
                request.revisionNote(),
                request.submitForReview()
        );
    }

    @PostMapping("/{runId}/dispatch")
    public ReplyDispatch dispatch(@PathVariable String runId) {
        return replySendLifecycleService.dispatch(runId);
    }

    @GetMapping("/{runId}/dispatches")
    public List<ReplyDispatch> listDispatches(@PathVariable String runId) {
        return replySendLifecycleService.findDispatches(runId);
    }

    @GetMapping("/{runId}/reviews")
    public List<ReviewRecord> listReviews(@PathVariable String runId) {
        return replyReviewLifecycleService.findReviews(runId);
    }

    @PostMapping("/{runId}/retry-dispatch")
    public ReplyDispatch retryDispatch(@PathVariable String runId, @RequestBody(required = false) RetryDispatchRequest request) {
        String reviewer = request == null || request.reviewer() == null || request.reviewer().isBlank()
                ? "demo-admin"
                : request.reviewer();
        String retryReason = request == null || request.retryReason() == null || request.retryReason().isBlank()
                ? "manual retry from demo admin endpoint"
                : request.retryReason();
        return replyDispatchCompensationService.retry(runId, reviewer, retryReason);
    }

    @PostMapping("/dispatches/retry-due")
    public DispatchRetryBatchResult retryDueDispatches() {
        return replyDispatchCompensationService.retryDueDispatches();
    }

    public record DemoMailRequest(
            String messageId,
            String threadId,
            @Email String from,
            @NotBlank String subject,
            @NotBlank String body
    ) {
    }

    public record SendApprovalRequest(
            String reviewer,
            String approvalNote
    ) {
    }

    public record SendRejectRequest(
            String reviewer,
            String rejectNote
    ) {
    }

    public record RetryDispatchRequest(
            String reviewer,
            String retryReason
    ) {
    }

    public record ReviseDraftRequest(
            String editor,
            @NotBlank String subject,
            @NotBlank String body,
            String revisionNote,
            boolean submitForReview
    ) {
    }

    private InboundMail buildInboundMail(DemoMailRequest request) {
        return new InboundMail(
                request.messageId() == null || request.messageId().isBlank() ? UUID.randomUUID().toString() : request.messageId(),
                request.threadId() == null || request.threadId().isBlank() ? UUID.randomUUID().toString() : request.threadId(),
                request.from(),
                request.subject(),
                request.body(),
                OffsetDateTime.now()
        );
    }
}
