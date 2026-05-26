package com.leak.intelligentcustomerchat.infrastructure.reply;

import com.leak.intelligentcustomerchat.app.reply.OutboundMailRequest;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSendResult;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSender;
import com.leak.intelligentcustomerchat.config.MailOutboundProperties;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public class SmtpOutboundMailSender implements OutboundMailSender {
    private final MailOutboundProperties properties;
    private final MailTransportClient mailTransportClient;

    public SmtpOutboundMailSender(MailOutboundProperties properties, MailTransportClient mailTransportClient) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.mailTransportClient = Objects.requireNonNull(mailTransportClient, "mailTransportClient must not be null");
    }

    @Override
    public OutboundMailSendResult send(OutboundMailRequest request) {
        try {
            validateProperties();
            Session session = createSession();
            MimeMessage message = buildMessage(session, request);
            String providerMessageId = extractMessageId(message);
            mailTransportClient.send(message);
            return OutboundMailSendResult.success(providerMessageId);
        } catch (Exception ex) {
            return OutboundMailSendResult.failed(buildFailureMessage(ex));
        }
    }

    private void validateProperties() {
        if (properties.host().isBlank()) {
            throw new IllegalStateException("smtp host must not be blank");
        }
        if (properties.fromAddress().isBlank()) {
            throw new IllegalStateException("smtp fromAddress must not be blank");
        }
        if (properties.authEnabled() && properties.username().isBlank()) {
            throw new IllegalStateException("smtp username must not be blank when auth is enabled");
        }
    }

    private Session createSession() {
        Properties sessionProperties = new Properties();
        sessionProperties.setProperty("mail.transport.protocol", "smtp");
        sessionProperties.setProperty("mail.smtp.host", properties.host());
        sessionProperties.setProperty("mail.smtp.port", String.valueOf(properties.port()));
        sessionProperties.setProperty("mail.smtp.auth", String.valueOf(properties.authEnabled()));
        sessionProperties.setProperty("mail.smtp.starttls.enable", String.valueOf(properties.startTlsEnabled()));
        sessionProperties.setProperty("mail.smtp.ssl.enable", String.valueOf(properties.sslEnabled()));
        sessionProperties.setProperty("mail.smtp.connectiontimeout", String.valueOf(properties.connectionTimeoutMillis()));
        sessionProperties.setProperty("mail.smtp.timeout", String.valueOf(properties.timeoutMillis()));
        sessionProperties.setProperty("mail.smtp.writetimeout", String.valueOf(properties.writeTimeoutMillis()));
        if (!properties.authEnabled()) {
            return Session.getInstance(sessionProperties);
        }
        return Session.getInstance(sessionProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(properties.username(), properties.password());
            }
        });
    }

    private MimeMessage buildMessage(Session session, OutboundMailRequest request) throws Exception {
        MimeMessage message = new MimeMessage(session);
        // 保持纯文本发信，先把最小闭环跑通，后续再扩展模板和富文本能力。
        message.setFrom(new InternetAddress(properties.fromAddress(), properties.fromName(), StandardCharsets.UTF_8.name()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.recipient(), false));
        message.setSubject(request.subject(), StandardCharsets.UTF_8.name());
        message.setText(request.body(), StandardCharsets.UTF_8.name());
        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }

    private String extractMessageId(MimeMessage message) throws MessagingException {
        String messageId = message.getHeader("Message-ID", null);
        return messageId == null || messageId.isBlank()
                ? "smtp-" + UUID.randomUUID()
                : messageId;
    }

    private String buildFailureMessage(Exception ex) {
        String detail = ex.getMessage();
        return detail == null || detail.isBlank()
                ? "smtp send failed: " + ex.getClass().getSimpleName()
                : "smtp send failed: " + detail;
    }
}
