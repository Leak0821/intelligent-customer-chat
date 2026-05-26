package com.leak.intelligentcustomerchat.app.reply;

public interface OutboundMailSender {
    OutboundMailSendResult send(OutboundMailRequest request);
}
