package com.leak.intelligentcustomerchat.infrastructure.persistence.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisWorkflowEventRepository implements WorkflowEventRepository {
    private final WorkflowEventMapper workflowEventMapper;

    public MybatisWorkflowEventRepository(WorkflowEventMapper workflowEventMapper) {
        this.workflowEventMapper = workflowEventMapper;
    }

    @Override
    public void save(WorkflowEvent event) {
        workflowEventMapper.insert(WorkflowEntityMapper.toEntity(event));
    }

    @Override
    public List<WorkflowEvent> findByRunId(String runId) {
        LambdaQueryWrapper<WorkflowEventEntity> query = new LambdaQueryWrapper<WorkflowEventEntity>()
                .eq(WorkflowEventEntity::getRunId, runId)
                .orderByAsc(WorkflowEventEntity::getCreatedAt);
        return workflowEventMapper.selectList(query).stream()
                .map(WorkflowEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<WorkflowEvent> findAll() {
        LambdaQueryWrapper<WorkflowEventEntity> query = new LambdaQueryWrapper<WorkflowEventEntity>()
                .orderByDesc(WorkflowEventEntity::getCreatedAt);
        return workflowEventMapper.selectList(query).stream()
                .map(WorkflowEntityMapper::toDomain)
                .toList();
    }
}
