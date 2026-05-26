package com.leak.intelligentcustomerchat.interfaces.scheduler;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.scheduler.xxl", name = "enabled", havingValue = "true")
public class XxlMailJobHandler {
    private final MailIngestionService mailIngestionService;

    public XxlMailJobHandler(MailIngestionService mailIngestionService) {
        this.mailIngestionService = mailIngestionService;
    }

    @XxlJob("mailPollingJobHandler")
    public void pollInbox() {
        MailPollingResult result = mailIngestionService.fetchAndProcess();
        XxlJobHelper.log("mail polling finished, fetched=%s, processed=%s, errors=%s",
                result.fetchedCount(), result.processedCount(), result.errors().size());
    }
}
