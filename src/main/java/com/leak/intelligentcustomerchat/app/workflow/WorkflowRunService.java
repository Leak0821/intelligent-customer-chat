package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowRunService {
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final WorkflowEventRecorder workflowEventRecorder;
    private final WorkflowStageExecutor workflowStageExecutor;

    public WorkflowRunService(WorkflowRunRepository workflowRunRepository,
                              WorkflowEventRepository workflowEventRepository,
                              WorkflowEventRecorder workflowEventRecorder,
                              WorkflowStageExecutor workflowStageExecutor) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.workflowEventRecorder = workflowEventRecorder;
        this.workflowStageExecutor = workflowStageExecutor;
    }

    public WorkflowRun start(InboundMail inboundMail) {
        WorkflowRun run = WorkflowRun.start(inboundMail.messageId(), inboundMail.threadId());
        workflowRunRepository.save(run);
        workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
        return workflowStageExecutor.execute(run, inboundMail);
    }

    public List<WorkflowRun> findAllRuns() {
        return workflowRunRepository.findAll();
    }

    public List<WorkflowEvent> findEvents(String runId) {
        return workflowEventRepository.findByRunId(runId);
    }
}
