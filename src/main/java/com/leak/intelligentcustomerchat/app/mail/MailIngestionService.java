package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.app.workflow.WorkflowRunService;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailFetchResult;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.infrastructure.mail.MailSourceAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailIngestionService {
    private final MailSourceAdapter mailSourceAdapter;
    private final WorkflowRunService workflowRunService;

    public MailIngestionService(MailSourceAdapter mailSourceAdapter, WorkflowRunService workflowRunService) {
        this.mailSourceAdapter = mailSourceAdapter;
        this.workflowRunService = workflowRunService;
    }

    public MailFetchResult fetchNewMails() {
        return mailSourceAdapter.fetchNewMails();
    }

    public WorkflowRun process(InboundMail inboundMail) {
        return workflowRunService.start(inboundMail);
    }

    public List<WorkflowRun> process(MailFetchResult fetchResult) {
        return fetchResult.mails().stream()
                .map(this::process)
                .toList();
    }
}
