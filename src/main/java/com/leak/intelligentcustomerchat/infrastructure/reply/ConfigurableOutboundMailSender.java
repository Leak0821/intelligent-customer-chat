package com.leak.intelligentcustomerchat.infrastructure.reply;

import com.leak.intelligentcustomerchat.app.reply.OutboundMailRequest;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSendResult;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSender;
import com.leak.intelligentcustomerchat.config.MailOutboundProperties;
import com.leak.intelligentcustomerchat.config.MailOutboundProvider;
import org.springframework.stereotype.Component;

@Component
public class ConfigurableOutboundMailSender implements OutboundMailSender {
    private final MailOutboundProperties properties;
    private final NoopOutboundMailSender noopOutboundMailSender;
    private final SmtpOutboundMailSender smtpOutboundMailSender;

    public ConfigurableOutboundMailSender(MailOutboundProperties properties,
                                          MailTransportClient mailTransportClient) {
        this.properties = properties;
        this.noopOutboundMailSender = new NoopOutboundMailSender(properties);
        this.smtpOutboundMailSender = new SmtpOutboundMailSender(properties, mailTransportClient);
    }

    @Override
    public OutboundMailSendResult send(OutboundMailRequest request) {
        if (!properties.enabled()) {
            return noopOutboundMailSender.send(request);
        }
        if (properties.provider() == MailOutboundProvider.SMTP) {
            return smtpOutboundMailSender.send(request);
        }
        return noopOutboundMailSender.send(request);
    }
}
