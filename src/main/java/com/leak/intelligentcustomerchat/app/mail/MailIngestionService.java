package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailFetchResult;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.infrastructure.mail.MailSourceAdapter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MailIngestionService {
    private final MailSourceAdapter mailSourceAdapter;
    private final MailReceiptRepository mailReceiptRepository;
    private final WorkflowRunService workflowRunService;

    public MailIngestionService(MailSourceAdapter mailSourceAdapter,
                                MailReceiptRepository mailReceiptRepository,
                                WorkflowRunService workflowRunService) {
        this.mailSourceAdapter = mailSourceAdapter;
        this.mailReceiptRepository = mailReceiptRepository;
        this.workflowRunService = workflowRunService;
    }

    public MailFetchResult fetchNewMails() {
        return mailSourceAdapter.fetchNewMails();
    }

    public MailPollingResult fetchAndEnqueue() {
        MailFetchResult fetchResult = fetchNewMails();
        return enqueue(fetchResult);
    }

    public MailPollingResult fetchAndProcess() {
        MailPollingResult enqueueResult = fetchAndEnqueue();
        MailPollingResult processResult = processPendingReceipts(Math.max(enqueueResult.queuedCount(), 1));
        return new MailPollingResult(
                enqueueResult.fetchedCount(),
                enqueueResult.queuedCount(),
                processResult.processedCount(),
                processResult.failedCount(),
                processResult.runIds(),
                mergeErrors(enqueueResult.errors(), processResult.errors())
        );
    }

    public WorkflowRun process(InboundMail inboundMail) {
        MailReceipt receipt = resolveOrCreateReceipt(inboundMail);
        receipt.markQueued();
        mailReceiptRepository.save(receipt);
        return processReceipt(receipt);
    }

    public List<WorkflowRun> process(MailFetchResult fetchResult) {
        List<WorkflowRun> runs = new ArrayList<>();
        for (InboundMail mail : fetchResult.mails()) {
            runs.add(process(mail));
        }
        return runs;
    }

    public MailPollingResult enqueue(MailFetchResult fetchResult) {
        int queuedCount = 0;
        for (InboundMail mail : fetchResult.mails()) {
            MailReceipt receipt = resolveOrCreateReceipt(mail);
            if (receipt.getStatus() == com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus.PROCESSED
                    ) {
                continue;
            }
            receipt.markQueued();
            mailReceiptRepository.save(receipt);
            queuedCount += 1;
        }
        return new MailPollingResult(
                fetchResult.mails().size(),
                queuedCount,
                0,
                0,
                List.of(),
                fetchResult.errors()
        );
    }

    public MailPollingResult processPendingReceipts(int limit) {
        List<WorkflowRun> runs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int failedCount = 0;
        for (MailReceipt receipt : mailReceiptRepository.findPendingForProcessing(limit)) {
            try {
                WorkflowRun run = processReceipt(receipt);
                runs.add(run);
            } catch (RuntimeException ex) {
                failedCount += 1;
                errors.add("failed to process receipt messageId=" + receipt.getMessageId() + ": " + ex.getMessage());
            }
        }
        return new MailPollingResult(
                0,
                0,
                runs.size(),
                failedCount,
                runs.stream().map(WorkflowRun::getRunId).toList(),
                List.copyOf(errors)
        );
    }

    public MailReceipt requeueReceipt(String messageId) {
        MailReceipt receipt = mailReceiptRepository.findByMessageId(messageId)
                .orElseThrow(() -> new NoSuchElementException("mail receipt not found for messageId=" + messageId));
        receipt.markQueued();
        return mailReceiptRepository.save(receipt);
    }

    public List<MailReceipt> listRecentReceipts(int limit) {
        return mailReceiptRepository.findRecent(limit);
    }

    private MailReceipt resolveOrCreateReceipt(InboundMail inboundMail) {
        return mailReceiptRepository.findByMessageId(inboundMail.messageId())
                .orElseGet(() -> mailReceiptRepository.save(MailReceipt.manual(UUID.randomUUID().toString(), inboundMail)));
    }

    private WorkflowRun processReceipt(MailReceipt receipt) {
        if (receipt.getRawBody().isBlank()) {
            receipt.markFailed("mail raw body snapshot is blank");
            mailReceiptRepository.save(receipt);
            throw new IllegalStateException("mail raw body snapshot is blank");
        }
        try {
            WorkflowRun run = workflowRunService.start(receipt.toInboundMail());
            receipt.markProcessed(run.getRunId());
            mailReceiptRepository.save(receipt);
            return run;
        } catch (RuntimeException ex) {
            receipt.markFailed(ex.getMessage() == null ? "workflow start failed" : ex.getMessage());
            mailReceiptRepository.save(receipt);
            throw ex;
        }
    }

    private List<String> mergeErrors(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }
}
