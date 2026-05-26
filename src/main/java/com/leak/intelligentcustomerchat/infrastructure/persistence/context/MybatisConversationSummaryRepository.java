package com.leak.intelligentcustomerchat.infrastructure.persistence.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummary;
import com.leak.intelligentcustomerchat.domain.context.ConversationSummaryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisConversationSummaryRepository implements ConversationSummaryRepository {
    private final ConversationSummaryMapper conversationSummaryMapper;

    public MybatisConversationSummaryRepository(ConversationSummaryMapper conversationSummaryMapper) {
        this.conversationSummaryMapper = conversationSummaryMapper;
    }

    @Override
    public ConversationSummary save(ConversationSummary summary) {
        conversationSummaryMapper.insert(ConversationSummaryEntityMapper.toEntity(summary));
        return summary;
    }

    @Override
    public Optional<ConversationSummary> findLatestByThreadId(String threadId) {
        LambdaQueryWrapper<ConversationSummaryEntity> query = new LambdaQueryWrapper<ConversationSummaryEntity>()
                .eq(ConversationSummaryEntity::getThreadId, threadId)
                .orderByDesc(ConversationSummaryEntity::getCreatedAt)
                .last("limit 1");
        ConversationSummaryEntity entity = conversationSummaryMapper.selectOne(query);
        return entity == null ? Optional.empty() : Optional.of(ConversationSummaryEntityMapper.toDomain(entity));
    }
}
