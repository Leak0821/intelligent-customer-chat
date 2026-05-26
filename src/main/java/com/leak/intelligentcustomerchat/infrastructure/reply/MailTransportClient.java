package com.leak.intelligentcustomerchat.infrastructure.reply;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public interface MailTransportClient {
    void send(MimeMessage message) throws MessagingException;
}
