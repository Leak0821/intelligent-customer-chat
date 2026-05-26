package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.config.MailProperties;
import com.leak.intelligentcustomerchat.config.XxlJobProperties;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus;
import org.springframework.stereotype.Service;

@Service
public class MailOpsOverviewService {
    private final MailIngestionService mailIngestionService;
    private final MailReceiptRepository mailReceiptRepository;
    private final MailProperties mailProperties;
    private final XxlJobProperties xxlJobProperties;

    public MailOpsOverviewService(MailIngestionService mailIngestionService,
                                  MailReceiptRepository mailReceiptRepository,
                                  MailProperties mailProperties,
                                  XxlJobProperties xxlJobProperties) {
        this.mailIngestionService = mailIngestionService;
        this.mailReceiptRepository = mailReceiptRepository;
        this.mailProperties = mailProperties;
        this.xxlJobProperties = xxlJobProperties;
    }

    public MailOpsOverviewView overview(int recentLimit) {
        return new MailOpsOverviewView(
                mailProperties.enabled(),
                mailProperties.source(),
                mailProperties.pollingEnabled(),
                resolveSchedulerMode(),
                mailProperties.pollSize(),
                mailProperties.pollIntervalMillis(),
                new MailReceiptStatsView(
                        mailReceiptRepository.countAll(),
                        mailReceiptRepository.countByStatus(MailReceiptStatus.FETCHED),
                        mailReceiptRepository.countByStatus(MailReceiptStatus.QUEUED),
                        mailReceiptRepository.countByStatus(MailReceiptStatus.PROCESSED),
                        mailReceiptRepository.countByStatus(MailReceiptStatus.FAILED)
                ),
                mailIngestionService.listRecentReceipts(recentLimit)
        );
    }

    private String resolveSchedulerMode() {
        if (!mailProperties.enabled()) {
            return "mail-disabled";
        }
        if (!mailProperties.pollingEnabled()) {
            return "manual-trigger";
        }
        if (xxlJobProperties.enabled()) {
            return "xxl-job";
        }
        return "local-scheduler";
    }
}
