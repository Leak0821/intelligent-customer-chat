package com.leak.intelligentcustomerchat.app.mail;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

public interface MailCleaner {
    InboundMail clean(InboundMail mail);
}
