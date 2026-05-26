package com.leak.intelligentcustomerchat.infrastructure.persistence.reply;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisReplyDraftRepository implements ReplyDraftRepository {
    private final ReplyDraftMapper replyDraftMapper;

    public MybatisReplyDraftRepository(ReplyDraftMapper replyDraftMapper) {
        this.replyDraftMapper = replyDraftMapper;
    }

    @Override
    public ReplyDraft save(ReplyDraft draft) {
        ReplyDraftEntity entity = ReplyDraftEntityMapper.toEntity(draft);
        if (replyDraftMapper.selectById(draft.getDraftId()) == null) {
            replyDraftMapper.insert(entity);
        } else {
            replyDraftMapper.updateById(entity);
        }
        return draft;
    }

    @Override
    public Optional<ReplyDraft> findByRunId(String runId) {
        LambdaQueryWrapper<ReplyDraftEntity> query = new LambdaQueryWrapper<ReplyDraftEntity>()
                .eq(ReplyDraftEntity::getRunId, runId)
                .orderByDesc(ReplyDraftEntity::getCreatedAt)
                .last("limit 1");
        ReplyDraftEntity entity = replyDraftMapper.selectOne(query);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(ReplyDraftEntityMapper.toDomain(entity));
    }
}
