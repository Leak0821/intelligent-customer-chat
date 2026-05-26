package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.reply.ReplySendLifecycleService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowAnalysisService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowAnalysisView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowReplayView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
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
    private final ReplySendLifecycleService replySendLifecycleService;

    public WorkflowDemoController(MailIngestionService mailIngestionService,
                                  WorkflowRunService workflowRunService,
                                  WorkflowAnalysisService workflowAnalysisService,
                                  ReplySendLifecycleService replySendLifecycleService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
        this.workflowAnalysisService = workflowAnalysisService;
        this.replySendLifecycleService = replySendLifecycleService;
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
        String approvalNote = request == null || request.approvalNote() == null || request.approvalNote().isBlank()
                ? "approved for send from demo admin endpoint"
                : request.approvalNote();
        return replySendLifecycleService.approveForSend(runId, approvalNote);
    }

    @PostMapping("/{runId}/dispatch")
    public ReplyDispatch dispatch(@PathVariable String runId) {
        return replySendLifecycleService.dispatch(runId);
    }

    @GetMapping("/{runId}/dispatches")
    public List<ReplyDispatch> listDispatches(@PathVariable String runId) {
        return replySendLifecycleService.findDispatches(runId);
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
            String approvalNote
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
