package com.leak.intelligentcustomerchat.domain.mail;

import java.util.List;

public record MailPollingResult(
        int fetchedCount,
        int queuedCount,
        int processedCount,
        int failedCount,
        List<String> runIds,
        List<String> errors
) {
    public MailPollingResult {
        runIds = List.copyOf(runIds);
        errors = List.copyOf(errors);
    }
}
