package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.mail.MailOpsOverviewService;
import com.leak.intelligentcustomerchat.app.mail.MailOpsOverviewView;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/mail")
public class MailAdminController {
    private final MailIngestionService mailIngestionService;
    private final MailOpsOverviewService mailOpsOverviewService;

    public MailAdminController(MailIngestionService mailIngestionService,
                               MailOpsOverviewService mailOpsOverviewService) {
        this.mailIngestionService = mailIngestionService;
        this.mailOpsOverviewService = mailOpsOverviewService;
    }

    @GetMapping("/overview")
    public MailOpsOverviewView overview(@RequestParam(defaultValue = "10") int recentLimit) {
        return mailOpsOverviewService.overview(recentLimit);
    }

    @PostMapping("/manual-enqueue")
    public MailReceipt manualEnqueue(@RequestBody ManualMailRequest request) {
        return mailIngestionService.enqueueManual(buildInboundMail(request));
    }

    @PostMapping("/poll")
    public MailPollingResult pollInbox() {
        return mailIngestionService.fetchAndEnqueue();
    }

    @PostMapping("/process-pending")
    public MailPollingResult processPending(@RequestParam(defaultValue = "20") int limit) {
        return mailIngestionService.processPendingReceipts(limit);
    }

    @PostMapping("/poll-and-process")
    public MailPollingResult pollAndProcess() {
        return mailIngestionService.fetchAndProcess();
    }

    @PostMapping("/receipts/{messageId}/requeue")
    public MailReceipt requeue(@PathVariable String messageId) {
        return mailIngestionService.requeueReceipt(messageId);
    }

    @GetMapping("/receipts/{messageId}")
    public MailReceipt getReceipt(@PathVariable String messageId) {
        return mailIngestionService.findReceipt(messageId);
    }

    @PostMapping("/receipts/{messageId}/process")
    public WorkflowRun processReceipt(@PathVariable String messageId) {
        return mailIngestionService.processReceiptByMessageId(messageId);
    }

    @GetMapping("/receipts")
    public List<MailReceipt> listReceipts(@RequestParam(defaultValue = "20") int limit) {
        return mailIngestionService.listRecentReceipts(limit);
    }

    public record ManualMailRequest(
            String messageId,
            String threadId,
            @Email String from,
            @NotBlank String subject,
            @NotBlank String body
    ) {
    }

    private InboundMail buildInboundMail(ManualMailRequest request) {
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
