package com.leak.intelligentcustomerchat.interfaces.scheduler;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.app.reply.DispatchRetryBatchResult;
import com.leak.intelligentcustomerchat.app.reply.ReplyDispatchCompensationService;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.scheduler.xxl", name = "enabled", havingValue = "true")
public class XxlMailJobHandler {
    private final MailIngestionService mailIngestionService;
    private final ReplyDispatchCompensationService replyDispatchCompensationService;

    public XxlMailJobHandler(MailIngestionService mailIngestionService,
                             ReplyDispatchCompensationService replyDispatchCompensationService) {
        this.mailIngestionService = mailIngestionService;
        this.replyDispatchCompensationService = replyDispatchCompensationService;
    }

    @XxlJob("mailPollingJobHandler")
    public void pollInbox() {
        MailPollingResult result = mailIngestionService.fetchAndProcess();
        XxlJobHelper.log("mail polling finished, fetched=%s, queued=%s, processed=%s, failed=%s, errors=%s",
                result.fetchedCount(), result.queuedCount(), result.processedCount(), result.failedCount(), result.errors().size());
    }

    @XxlJob("replyDispatchRetryJobHandler")
    public void retryDueDispatches() {
        DispatchRetryBatchResult result = replyDispatchCompensationService.retryDueDispatches();
        XxlJobHelper.log("dispatch retry finished, processed=%s, sent=%s, retryPending=%s, failedFinal=%s",
                result.processedCount(), result.sentCount(), result.retryPendingCount(), result.failedFinalCount());
    }
}
