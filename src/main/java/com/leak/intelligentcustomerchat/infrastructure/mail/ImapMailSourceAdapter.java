package com.leak.intelligentcustomerchat.infrastructure.mail;

import com.leak.intelligentcustomerchat.config.MailProperties;
import com.leak.intelligentcustomerchat.domain.mail.MailFetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ImapMailSourceAdapter implements MailSourceAdapter {
    private static final Logger log = LoggerFactory.getLogger(ImapMailSourceAdapter.class);

    private final MailProperties mailProperties;

    public ImapMailSourceAdapter(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Override
    public MailFetchResult fetchNewMails() {
        // 这里先占位 IMAP 接入点，避免首批骨架直接耦合真实邮箱；后续会补 UID 去重、附件过滤、HTML 转文本等细节。
        log.info("IMAP adapter placeholder invoked, source={}, host={}, folder={}",
                mailProperties.source(), mailProperties.host(), mailProperties.folder());
        return MailFetchResult.empty();
    }
}
