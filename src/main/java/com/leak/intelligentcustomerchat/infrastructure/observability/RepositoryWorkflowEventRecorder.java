package com.leak.intelligentcustomerchat.infrastructure.observability;

import com.leak.intelligentcustomerchat.app.workflow.WorkflowEventRecorder;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import org.springframework.stereotype.Component;

@Component
public class RepositoryWorkflowEventRecorder implements WorkflowEventRecorder {
    private final WorkflowEventRepository workflowEventRepository;

    public RepositoryWorkflowEventRecorder(WorkflowEventRepository workflowEventRepository) {
        this.workflowEventRepository = workflowEventRepository;
    }

    @Override
    public void record(WorkflowEvent event) {
        workflowEventRepository.save(event);
    }
}
