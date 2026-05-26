package com.leak.intelligentcustomerchat.infrastructure.mail;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ImapMessageMapperTest {
    private final ImapMessageMapper mapper = new ImapMessageMapper();

    @Test
    void shouldMapPlainTextMessage() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress("buyer@example.com"));
        message.setSubject("Need help");
        message.setText("Hello,\nI need help with my order.");
        message.saveChanges();
        message.setHeader("Message-ID", "<msg-plain-1>");

        InboundMail inboundMail = mapper.toInboundMail(message, 101L, "imap:test", "INBOX");

        assertThat(inboundMail.messageId()).isEqualTo("<msg-plain-1>");
        assertThat(inboundMail.threadId()).isEqualTo("<msg-plain-1>");
        assertThat(inboundMail.from()).isEqualTo("buyer@example.com");
        assertThat(inboundMail.rawBody()).contains("I need help with my order.");
    }

    @Test
    void shouldPreferPlainTextAndFallbackThreadFromReferences() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress("buyer@example.com"));
        message.setSubject("Product question");
        message.setHeader("References", "<parent-1> <thread-root-9>");

        MimeBodyPart plainPart = new MimeBodyPart();
        plainPart.setText("Can you recommend a product for my living room?");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<p>Ignore this <b>HTML</b> variant.</p>", "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(plainPart);
        message.setContent(multipart);
        message.saveChanges();

        InboundMail inboundMail = mapper.toInboundMail(message, 102L, "imap:test", "INBOX");

        assertThat(inboundMail.threadId()).isEqualTo("<thread-root-9>");
        assertThat(inboundMail.rawBody()).contains("recommend a product");
        assertThat(inboundMail.rawBody()).doesNotContain("<b>");
    }
}
