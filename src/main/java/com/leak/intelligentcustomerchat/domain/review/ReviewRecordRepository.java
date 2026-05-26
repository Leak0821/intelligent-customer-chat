package com.leak.intelligentcustomerchat.domain.review;

import java.util.List;
import java.util.Optional;

public interface ReviewRecordRepository {
    ReviewRecord save(ReviewRecord reviewRecord);

    List<ReviewRecord> findByRunId(String runId);

    Optional<ReviewRecord> findLatestApprovalByRunId(String runId);
}
