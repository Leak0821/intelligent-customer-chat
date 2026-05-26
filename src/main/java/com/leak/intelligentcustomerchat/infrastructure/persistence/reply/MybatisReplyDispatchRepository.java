package com.leak.intelligentcustomerchat.infrastructure.persistence.reply;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisReplyDispatchRepository implements ReplyDispatchRepository {
    private final ReplyDispatchMapper replyDispatchMapper;

    public MybatisReplyDispatchRepository(ReplyDispatchMapper replyDispatchMapper) {
        this.replyDispatchMapper = replyDispatchMapper;
    }

    @Override
    public ReplyDispatch save(ReplyDispatch dispatch) {
        ReplyDispatchEntity entity = ReplyDispatchEntityMapper.toEntity(dispatch);
        if (replyDispatchMapper.selectById(dispatch.getDispatchId()) == null) {
            replyDispatchMapper.insert(entity);
        } else {
            replyDispatchMapper.updateById(entity);
        }
        return dispatch;
    }

    @Override
    public List<ReplyDispatch> findByRunId(String runId) {
        LambdaQueryWrapper<ReplyDispatchEntity> query = new LambdaQueryWrapper<ReplyDispatchEntity>()
                .eq(ReplyDispatchEntity::getRunId, runId)
                .orderByAsc(ReplyDispatchEntity::getCreatedAt);
        return replyDispatchMapper.selectList(query).stream()
                .map(ReplyDispatchEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ReplyDispatch> findLatestByRunId(String runId) {
        LambdaQueryWrapper<ReplyDispatchEntity> query = new LambdaQueryWrapper<ReplyDispatchEntity>()
                .eq(ReplyDispatchEntity::getRunId, runId)
                .orderByDesc(ReplyDispatchEntity::getCreatedAt)
                .last("limit 1");
        ReplyDispatchEntity entity = replyDispatchMapper.selectOne(query);
        return entity == null ? Optional.empty() : Optional.of(ReplyDispatchEntityMapper.toDomain(entity));
    }
}
