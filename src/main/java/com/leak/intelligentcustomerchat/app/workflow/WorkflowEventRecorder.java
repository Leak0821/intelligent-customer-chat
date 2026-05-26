package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;

public interface WorkflowEventRecorder {
    void record(WorkflowEvent event);
}
