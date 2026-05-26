package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowReplayView;
import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
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

    public WorkflowDemoController(MailIngestionService mailIngestionService, WorkflowRunService workflowRunService) {
        this.mailIngestionService = mailIngestionService;
        this.workflowRunService = workflowRunService;
    }

    @PostMapping("/demo")
    public WorkflowRun triggerDemo(@RequestBody DemoMailRequest request) {
        // 管理口只用于本地验证最小闭环，不代表最终邮件接入协议。
        InboundMail inboundMail = new InboundMail(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                request.from(),
                request.subject(),
                request.body(),
                OffsetDateTime.now()
        );
        return mailIngestionService.process(inboundMail);
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

    public record DemoMailRequest(
            @Email String from,
            @NotBlank String subject,
            @NotBlank String body
    ) {
    }
}
