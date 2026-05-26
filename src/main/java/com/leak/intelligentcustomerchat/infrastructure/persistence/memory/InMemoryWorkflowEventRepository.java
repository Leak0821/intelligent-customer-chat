package com.leak.intelligentcustomerchat.infrastructure.persistence.memory;

import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryWorkflowEventRepository implements WorkflowEventRepository {
    private final CopyOnWriteArrayList<WorkflowEvent> storage = new CopyOnWriteArrayList<>();

    @Override
    public void save(WorkflowEvent event) {
        storage.add(event);
    }

    @Override
    public List<WorkflowEvent> findByRunId(String runId) {
        return storage.stream()
                .filter(event -> event.runId().equals(runId))
                .sorted(Comparator.comparing(WorkflowEvent::createdAt))
                .toList();
    }

    @Override
    public List<WorkflowEvent> findAll() {
        return new ArrayList<>(storage);
    }
}
