package com.leak.intelligentcustomerchat.domain.reply;

import java.util.List;
import java.util.Optional;

public interface ReplyDispatchRepository {
    ReplyDispatch save(ReplyDispatch dispatch);

    List<ReplyDispatch> findByRunId(String runId);

    Optional<ReplyDispatch> findLatestByRunId(String runId);
}
