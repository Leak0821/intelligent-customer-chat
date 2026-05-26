package com.leak.intelligentcustomerchat.domain.workflow;

import java.util.List;
import java.util.Optional;

public interface WorkflowRunRepository {
    WorkflowRun save(WorkflowRun run);

    Optional<WorkflowRun> findByRunId(String runId);

    Optional<WorkflowRun> findLatestByMessageId(String messageId);

    List<WorkflowRun> findAll();
}
