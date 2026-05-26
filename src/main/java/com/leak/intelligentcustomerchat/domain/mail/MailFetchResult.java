package com.leak.intelligentcustomerchat.domain.mail;

import java.util.List;

public record MailFetchResult(
        List<InboundMail> mails,
        List<String> errors
) {
    public static MailFetchResult empty() {
        return new MailFetchResult(List.of(), List.of());
    }
}
