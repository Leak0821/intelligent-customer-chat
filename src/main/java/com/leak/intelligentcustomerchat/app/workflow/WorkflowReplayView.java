package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;

import java.util.List;
import java.util.Objects;

public record WorkflowReplayView(
        WorkflowRun run,
        List<WorkflowEvent> events,
        ReplyDraft latestDraft
) {
    public WorkflowReplayView {
        Objects.requireNonNull(run, "run must not be null");
        events = List.copyOf(events);
    }
}
