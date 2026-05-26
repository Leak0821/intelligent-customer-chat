package com.leak.intelligentcustomerchat.app.mail;

public record MailReceiptStatsView(
        long total,
        long fetched,
        long queued,
        long processed,
        long failed
) {
}
