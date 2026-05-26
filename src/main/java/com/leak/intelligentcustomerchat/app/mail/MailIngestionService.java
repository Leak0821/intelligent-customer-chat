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

    public MailPollingResult fetchAndProcess() {
        MailFetchResult fetchResult = fetchNewMails();
        List<WorkflowRun> runs = process(fetchResult);
        return new MailPollingResult(
                fetchResult.mails().size(),
                runs.size(),
                runs.stream().map(WorkflowRun::getRunId).toList(),
                fetchResult.errors()
        );
    }

    public WorkflowRun process(InboundMail inboundMail) {
        WorkflowRun run = workflowRunService.start(inboundMail);
        mailReceiptRepository.findByMessageId(inboundMail.messageId())
                .ifPresent(receipt -> {
                    receipt.markProcessed(run.getRunId());
                    mailReceiptRepository.save(receipt);
                });
        return run;
    }

    public List<WorkflowRun> process(MailFetchResult fetchResult) {
        List<WorkflowRun> runs = new ArrayList<>();
        for (InboundMail mail : fetchResult.mails()) {
            runs.add(process(mail));
        }
        return runs;
    }

    public List<MailReceipt> listRecentReceipts(int limit) {
        return mailReceiptRepository.findRecent(limit);
    }
}
