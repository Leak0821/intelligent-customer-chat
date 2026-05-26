package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mail")
public class MailAdminController {
    private final MailIngestionService mailIngestionService;

    public MailAdminController(MailIngestionService mailIngestionService) {
        this.mailIngestionService = mailIngestionService;
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
    public MailReceipt requeue(@org.springframework.web.bind.annotation.PathVariable String messageId) {
        return mailIngestionService.requeueReceipt(messageId);
    }

    @GetMapping("/receipts")
    public List<MailReceipt> listReceipts(@RequestParam(defaultValue = "20") int limit) {
        return mailIngestionService.listRecentReceipts(limit);
    }
}
