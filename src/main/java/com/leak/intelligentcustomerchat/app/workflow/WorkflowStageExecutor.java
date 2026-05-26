package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.config.WorkflowProperties;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStageExecutor {
    private final MailCleaner mailCleaner;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEventRecorder workflowEventRecorder;
    private final WorkflowProperties workflowProperties;

    public WorkflowStageExecutor(MailCleaner mailCleaner,
                                 WorkflowRunRepository workflowRunRepository,
                                 WorkflowEventRecorder workflowEventRecorder,
                                 WorkflowProperties workflowProperties) {
        this.mailCleaner = mailCleaner;
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRecorder = workflowEventRecorder;
        this.workflowProperties = workflowProperties;
    }

    public WorkflowRun execute(WorkflowRun run, InboundMail inboundMail) {
        try {
            // Slice 1 先固定成“清洗后直接完成”的最小闭环，后续再逐步插入意图识别、RAG、订单查询、回复生成等阶段。
            InboundMail cleanedMail = mailCleaner.clean(inboundMail);
            run.moveTo(com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.MAIL_CLEANED,
                    "mail cleaned, body length=" + cleanedMail.rawBody().length());
            workflowRunRepository.save(run);
            workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));

            if (workflowProperties.autoCompleteEmptyChain()) {
                run.complete("slice-1 empty workflow completed");
                workflowRunRepository.save(run);
                workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
            }
            return run;
        } catch (RuntimeException ex) {
            // 这里统一把异常折叠为工作流事件，后续接入告警、重试和死信队列时仍然沿用这个出口。
            run.block("slice-1 workflow blocked: " + ex.getMessage());
            workflowRunRepository.save(run);
            workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
            return run;
        }
    }
}
