package com.leak.intelligentcustomerchat.domain.reply;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReplyDispatchRepository {
    ReplyDispatch save(ReplyDispatch dispatch);

    List<ReplyDispatch> findByRunId(String runId);

    Optional<ReplyDispatch> findLatestByRunId(String runId);

    List<ReplyDispatch> findRetryableDueBefore(OffsetDateTime dueBefore, int limit);
}
