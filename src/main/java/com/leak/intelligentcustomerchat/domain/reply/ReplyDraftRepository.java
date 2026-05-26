package com.leak.intelligentcustomerchat.domain.reply;

import java.util.Optional;

public interface ReplyDraftRepository {
    ReplyDraft save(ReplyDraft draft);

    Optional<ReplyDraft> findByRunId(String runId);
}
