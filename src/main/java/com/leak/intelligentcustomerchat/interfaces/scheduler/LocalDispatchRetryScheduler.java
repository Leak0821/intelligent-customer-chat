package com.leak.intelligentcustomerchat.interfaces.scheduler;

import com.leak.intelligentcustomerchat.app.reply.DispatchRetryBatchResult;
import com.leak.intelligentcustomerchat.app.reply.ReplyDispatchCompensationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(
        "'${app.mail.dispatch-retry.enabled:true}' == 'true' && '${app.mail.dispatch-retry.local-scheduler-enabled:true}' == 'true' && '${app.scheduler.xxl.enabled:false}' == 'false'"
)
public class LocalDispatchRetryScheduler {
    private static final Logger log = LoggerFactory.getLogger(LocalDispatchRetryScheduler.class);

    private final ReplyDispatchCompensationService replyDispatchCompensationService;

    public LocalDispatchRetryScheduler(ReplyDispatchCompensationService replyDispatchCompensationService) {
        this.replyDispatchCompensationService = replyDispatchCompensationService;
    }

    // 先保留本地补偿调度入口，后续切换到 XXL-JOB 时不需要改服务层。
    @Scheduled(fixedDelayString = "${app.mail.dispatch-retry.local-fixed-delay-millis:60000}")
    public void retryDueDispatches() {
        DispatchRetryBatchResult result = replyDispatchCompensationService.retryDueDispatches();
        if (result.processedCount() == 0) {
            return;
        }
        log.info("local dispatch retry finished, processed={}, sent={}, retryPending={}, failedFinal={}",
                result.processedCount(), result.sentCount(), result.retryPendingCount(), result.failedFinalCount());
    }
}
