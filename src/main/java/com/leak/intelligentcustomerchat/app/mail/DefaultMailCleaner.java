package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Component;

@Component
public class DefaultMailCleaner implements MailCleaner {

    @Override
    public InboundMail clean(InboundMail mail) {
        String cleanedBody = mail.rawBody()
                .replace("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return mail.withRawBody(cleanedBody);
    }
}
