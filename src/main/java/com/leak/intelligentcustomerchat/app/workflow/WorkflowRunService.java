package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkflowRunService {
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final ReplyDraftRepository replyDraftRepository;
    private final WorkflowEventRecorder workflowEventRecorder;
    private final WorkflowStageExecutor workflowStageExecutor;

    public WorkflowRunService(WorkflowRunRepository workflowRunRepository,
                              WorkflowEventRepository workflowEventRepository,
                              ReplyDraftRepository replyDraftRepository,
                              WorkflowEventRecorder workflowEventRecorder,
                              WorkflowStageExecutor workflowStageExecutor) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.workflowEventRecorder = workflowEventRecorder;
        this.workflowStageExecutor = workflowStageExecutor;
    }

    public WorkflowRun start(InboundMail inboundMail) {
        WorkflowRun run = WorkflowRun.start(inboundMail.messageId(), inboundMail.threadId());
        workflowRunRepository.save(run);
        workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
        return workflowStageExecutor.execute(run, inboundMail);
    }

    public List<WorkflowRun> findAllRuns() {
        return workflowRunRepository.findAll();
    }

    public Optional<WorkflowRun> findRun(String runId) {
        return workflowRunRepository.findByRunId(runId);
    }

    public Optional<WorkflowReplayView> findReplay(String runId) {
        return workflowRunRepository.findByRunId(runId)
                .map(this::buildReplayView);
    }

    public Optional<WorkflowReplayView> findReplayByMessageId(String messageId) {
        return workflowRunRepository.findLatestByMessageId(messageId)
                .map(this::buildReplayView);
    }

    public List<WorkflowEvent> findEvents(String runId) {
        return workflowEventRepository.findByRunId(runId);
    }

    public Optional<ReplyDraft> findDraft(String runId) {
        return replyDraftRepository.findByRunId(runId);
    }

    private WorkflowReplayView buildReplayView(WorkflowRun run) {
        return new WorkflowReplayView(
                run,
                workflowEventRepository.findByRunId(run.getRunId()),
                replyDraftRepository.findByRunId(run.getRunId()).orElse(null)
        );
    }
}
