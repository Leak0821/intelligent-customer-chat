package com.leak.intelligentcustomerchat.infrastructure.reply;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Component;

@Component
public class JakartaMailTransportClient implements MailTransportClient {
    @Override
    public void send(MimeMessage message) throws MessagingException {
        Transport.send(message);
    }
}
