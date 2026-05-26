package com.leak.intelligentcustomerchat.infrastructure.reply;

import com.leak.intelligentcustomerchat.app.reply.OutboundMailRequest;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSendResult;
import com.leak.intelligentcustomerchat.app.reply.OutboundMailSender;
import com.leak.intelligentcustomerchat.config.MailOutboundProperties;

import java.util.UUID;

public class NoopOutboundMailSender implements OutboundMailSender {
    private final MailOutboundProperties properties;

    public NoopOutboundMailSender(MailOutboundProperties properties) {
        this.properties = properties;
    }

    @Override
    public OutboundMailSendResult send(OutboundMailRequest request) {
        if (!properties.enabled()) {
            // 第一阶段默认使用 no-op 发件器，保留完整发送链路，同时避免误发真实邮件。
            return OutboundMailSendResult.success("noop-disabled-" + UUID.randomUUID());
        }
        return OutboundMailSendResult.success("noop-enabled-" + UUID.randomUUID());
    }
}
