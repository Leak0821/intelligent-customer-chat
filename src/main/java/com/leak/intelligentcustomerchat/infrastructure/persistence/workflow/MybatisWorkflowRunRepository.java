package com.leak.intelligentcustomerchat.infrastructure.persistence.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisWorkflowRunRepository implements WorkflowRunRepository {
    private final WorkflowRunMapper workflowRunMapper;

    public MybatisWorkflowRunRepository(WorkflowRunMapper workflowRunMapper) {
        this.workflowRunMapper = workflowRunMapper;
    }

    @Override
    public WorkflowRun save(WorkflowRun run) {
        WorkflowRunEntity entity = WorkflowEntityMapper.toEntity(run);
        if (workflowRunMapper.selectById(run.getRunId()) == null) {
            workflowRunMapper.insert(entity);
        } else {
            workflowRunMapper.updateById(entity);
        }
        return run;
    }

    @Override
    public Optional<WorkflowRun> findByRunId(String runId) {
        WorkflowRunEntity entity = workflowRunMapper.selectById(runId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(WorkflowEntityMapper.toDomain(entity));
    }

    @Override
    public Optional<WorkflowRun> findLatestByMessageId(String messageId) {
        LambdaQueryWrapper<WorkflowRunEntity> query = new LambdaQueryWrapper<WorkflowRunEntity>()
                .eq(WorkflowRunEntity::getMessageId, messageId)
                .orderByDesc(WorkflowRunEntity::getCreatedAt)
                .last("limit 1");
        WorkflowRunEntity entity = workflowRunMapper.selectOne(query);
        return entity == null ? Optional.empty() : Optional.of(WorkflowEntityMapper.toDomain(entity));
    }

    @Override
    public List<WorkflowRun> findAll() {
        LambdaQueryWrapper<WorkflowRunEntity> query = new LambdaQueryWrapper<WorkflowRunEntity>()
                .orderByDesc(WorkflowRunEntity::getCreatedAt);
        return workflowRunMapper.selectList(query).stream()
                .map(WorkflowEntityMapper::toDomain)
                .toList();
    }
}
