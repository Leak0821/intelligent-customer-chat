package com.leak.intelligentcustomerchat.domain.workflow;

import java.util.List;

public interface WorkflowEventRepository {
    void save(WorkflowEvent event);

    List<WorkflowEvent> findByRunId(String runId);

    List<WorkflowEvent> findAll();
}
