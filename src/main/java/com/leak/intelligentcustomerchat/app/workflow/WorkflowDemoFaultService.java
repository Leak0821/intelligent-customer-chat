package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

@Service
public class WorkflowDemoFaultService {

    public void failIfNeeded(InboundMail inboundMail) {
        if (inboundMail.messageId() != null && inboundMail.messageId().startsWith("demo-blocked-")) {
            // 仅用于本地演示 BLOCKED 路径，避免靠真实业务文案去“伪造”系统级阻断。
            throw new IllegalStateException("demo blocked scenario triggered");
        }
    }
}
