package com.leak.intelligentcustomerchat.infrastructure.persistence.memory;

import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryWorkflowRunRepository implements WorkflowRunRepository {
    private final ConcurrentHashMap<String, WorkflowRun> storage = new ConcurrentHashMap<>();

    @Override
    public WorkflowRun save(WorkflowRun run) {
        storage.put(run.getRunId(), run);
        return run;
    }

    @Override
    public Optional<WorkflowRun> findByRunId(String runId) {
        return Optional.ofNullable(storage.get(runId));
    }

    @Override
    public List<WorkflowRun> findAll() {
        return new ArrayList<>(storage.values());
    }
}
