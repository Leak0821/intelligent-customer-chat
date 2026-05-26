package com.leak.intelligentcustomerchat.interfaces.scheduler;

import com.leak.intelligentcustomerchat.app.mail.MailIngestionService;
import com.leak.intelligentcustomerchat.config.MailProperties;
import com.leak.intelligentcustomerchat.domain.mail.MailPollingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(
        "'${app.mail.enabled:false}' == 'true' && '${app.mail.polling-enabled:false}' == 'true' && '${app.scheduler.xxl.enabled:false}' == 'false'"
)
public class LocalMailPollingScheduler {
    private static final Logger log = LoggerFactory.getLogger(LocalMailPollingScheduler.class);

    private final MailIngestionService mailIngestionService;
    private final MailProperties mailProperties;

    public LocalMailPollingScheduler(MailIngestionService mailIngestionService, MailProperties mailProperties) {
        this.mailIngestionService = mailIngestionService;
        this.mailProperties = mailProperties;
    }

    // 这里先用本地定时触发占住调度入口，后续再切换到 XXL-JOB handler。
    @Scheduled(fixedDelayString = "${app.mail.poll-interval-millis:60000}")
    public void pollInbox() {
        MailPollingResult result = mailIngestionService.fetchAndProcess();
        log.info("local mail polling finished, source={}, fetched={}, queued={}, processed={}, failed={}, errors={}",
                mailProperties.source(), result.fetchedCount(), result.queuedCount(), result.processedCount(), result.failedCount(), result.errors().size());
    }
}
