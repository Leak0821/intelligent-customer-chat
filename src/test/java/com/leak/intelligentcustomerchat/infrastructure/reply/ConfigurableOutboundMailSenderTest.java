package com.leak.intelligentcustomerchat.infrastructure.reply;

import com.leak.intelligentcustomerchat.app.reply.OutboundMailRequest;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSendResult;
import com.leak.intelligentcustomerchat.config.MailOutboundProperties;
import com.leak.intelligentcustomerchat.config.MailOutboundProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableOutboundMailSenderTest {

    @Test
    void shouldFallbackToNoopWhenOutboundDisabled() {
        ConfigurableOutboundMailSender sender = new ConfigurableOutboundMailSender(
                new MailOutboundProperties(
                        false,
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
                ),
                message -> {
                    throw new IllegalStateException("transport should not be used");
                }
        );

        OutboundMailSendResult result = sender.send(new OutboundMailRequest(
                "customer@example.com",
                "Subject",
                "Body"
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).startsWith("noop-disabled-");
    }

    @Test
    void shouldDelegateToSmtpWhenProviderIsSmtp() {
        ConfigurableOutboundMailSender sender = new ConfigurableOutboundMailSender(
                new MailOutboundProperties(
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
                ),
                message -> {
                }
        );

        OutboundMailSendResult result = sender.send(new OutboundMailRequest(
                "customer@example.com",
                "Subject",
                "Body"
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isNotBlank();
        assertThat(result.providerMessageId()).doesNotStartWith("noop-");
    }
}
