package com.leak.intelligentcustomerchat.infrastructure.reply;

import com.leak.intelligentcustomerchat.app.reply.OutboundMailRequest;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSendResult;
import com.leak.intelligentcustomerchat.config.MailOutboundProperties;
import com.leak.intelligentcustomerchat.config.MailOutboundProvider;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpOutboundMailSenderTest {

    @Test
    void shouldBuildAndSendMimeMessageThroughSmtpAdapter() throws Exception {
        CapturingMailTransportClient transportClient = new CapturingMailTransportClient();
        SmtpOutboundMailSender sender = new SmtpOutboundMailSender(
                smtpProperties(),
                transportClient
        );

        OutboundMailSendResult result = sender.send(new OutboundMailRequest(
                "customer@example.com",
                "Your reply",
                "Thank you for your email."
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isNotBlank();
        assertThat(transportClient.capturedMessage).isNotNull();
        assertThat(transportClient.capturedMessage.getFrom()[0].toString()).contains("support@example.com");
        assertThat(transportClient.capturedMessage.getRecipients(Message.RecipientType.TO)[0].toString()).contains("customer@example.com");
        assertThat(transportClient.capturedMessage.getSubject()).isEqualTo("Your reply");
        assertThat(transportClient.capturedMessage.getContent().toString()).contains("Thank you for your email.");
    }

    @Test
    void shouldReturnFailedResultWhenSmtpTransportThrows() {
        SmtpOutboundMailSender sender = new SmtpOutboundMailSender(
                smtpProperties(),
                message -> {
                    throw new MessagingException("smtp unavailable");
                }
        );

        OutboundMailSendResult result = sender.send(new OutboundMailRequest(
                "customer@example.com",
                "Your reply",
                "Thank you for your email."
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("smtp unavailable");
    }

    private MailOutboundProperties smtpProperties() {
        return new MailOutboundProperties(
                true,
                MailOutboundProvider.SMTP,
                "support@example.com",
                "Support Team",
                "smtp.example.com",
                587,
                "mailer@example.com",
                "secret",
                true,
                true,
                false,
                5000,
                5000,
                5000
        );
    }

    private static final class CapturingMailTransportClient implements MailTransportClient {
        private MimeMessage capturedMessage;

        @Override
        public void send(MimeMessage message) {
            this.capturedMessage = message;
        }
    }
}
