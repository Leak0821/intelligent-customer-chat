package com.leak.intelligentcustomerchat.infrastructure.persistence.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leak.intelligentcustomerchat.domain.review.ReviewAction;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisReviewRecordRepository implements ReviewRecordRepository {
    private final ReviewRecordMapper reviewRecordMapper;

    public MybatisReviewRecordRepository(ReviewRecordMapper reviewRecordMapper) {
        this.reviewRecordMapper = reviewRecordMapper;
    }

    @Override
    public ReviewRecord save(ReviewRecord reviewRecord) {
        reviewRecordMapper.insert(ReviewRecordEntityMapper.toEntity(reviewRecord));
        return reviewRecord;
    }

    @Override
    public List<ReviewRecord> findByRunId(String runId) {
        LambdaQueryWrapper<ReviewRecordEntity> query = new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getRunId, runId)
                .orderByAsc(ReviewRecordEntity::getCreatedAt);
        return reviewRecordMapper.selectList(query).stream()
                .map(ReviewRecordEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ReviewRecord> findLatestApprovalByRunId(String runId) {
        LambdaQueryWrapper<ReviewRecordEntity> query = new LambdaQueryWrapper<ReviewRecordEntity>()
                .eq(ReviewRecordEntity::getRunId, runId)
                .eq(ReviewRecordEntity::getAction, ReviewAction.APPROVE_SEND.name())
                .orderByDesc(ReviewRecordEntity::getCreatedAt)
                .last("limit 1");
        ReviewRecordEntity entity = reviewRecordMapper.selectOne(query);
        return entity == null ? Optional.empty() : Optional.of(ReviewRecordEntityMapper.toDomain(entity));
    }
}
