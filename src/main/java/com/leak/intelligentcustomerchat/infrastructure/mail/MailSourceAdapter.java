package com.leak.intelligentcustomerchat.infrastructure.mail;

import com.leak.intelligentcustomerchat.domain.mail.MailFetchResult;

public interface MailSourceAdapter {
    MailFetchResult fetchNewMails();
}
