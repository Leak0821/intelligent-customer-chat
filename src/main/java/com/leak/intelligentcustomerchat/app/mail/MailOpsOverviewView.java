package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;

import java.util.List;

public record MailOpsOverviewView(
        boolean mailEnabled,
        String source,
        boolean pollingEnabled,
        String schedulerMode,
        int pollSize,
        long pollIntervalMillis,
        MailReceiptStatsView receiptStats,
        List<MailReceipt> recentReceipts
) {
}
