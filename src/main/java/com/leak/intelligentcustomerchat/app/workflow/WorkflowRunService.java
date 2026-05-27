package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
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
    private final ReplyDispatchRepository replyDispatchRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final WorkflowEventRecorder workflowEventRecorder;
    private final WorkflowStageExecutor workflowStageExecutor;
    private final WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser;

    public WorkflowRunService(WorkflowRunRepository workflowRunRepository,
                              WorkflowEventRepository workflowEventRepository,
                              ReplyDraftRepository replyDraftRepository,
                              ReplyDispatchRepository replyDispatchRepository,
                              ReviewRecordRepository reviewRecordRepository,
                              WorkflowEventRecorder workflowEventRecorder,
                              WorkflowStageExecutor workflowStageExecutor,
                              WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.replyDraftRepository = replyDraftRepository;
        this.replyDispatchRepository = replyDispatchRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.workflowEventRecorder = workflowEventRecorder;
        this.workflowStageExecutor = workflowStageExecutor;
        this.workflowEvidenceSummaryParser = workflowEvidenceSummaryParser;
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
        List<WorkflowEvent> events = workflowEventRepository.findByRunId(run.getRunId());
        return new WorkflowReplayView(
                run,
                events,
                replyDraftRepository.findByRunId(run.getRunId()).orElse(null),
                buildReplayEvidence(events),
                replyDispatchRepository.findByRunId(run.getRunId()),
                reviewRecordRepository.findByRunId(run.getRunId())
        );
    }

    private WorkflowReplayEvidenceView buildReplayEvidence(List<WorkflowEvent> events) {
        String routingSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.INTENT_ROUTED)
                .orElse("intent route summary unavailable");
        String businessFactsSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.BUSINESS_FACTS_READY)
                .orElse("business facts summary unavailable");
        String knowledgeSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.KNOWLEDGE_READY)
                .orElse("knowledge summary unavailable");
        String replyDraftSummary = findEventSummary(events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage.REPLY_DRAFTED)
                .orElse("reply draft summary unavailable");

        var routingTokens = workflowEvidenceSummaryParser.parseTokens(routingSummary);
        var businessTokens = workflowEvidenceSummaryParser.parseTokens(businessFactsSummary);
        var knowledgeTokens = workflowEvidenceSummaryParser.parseTokens(knowledgeSummary);
        var replyTokens = workflowEvidenceSummaryParser.parseTokens(replyDraftSummary);
        String subIntent = workflowEvidenceSummaryParser.tokenOrDefault(routingTokens, "subIntent", "general_inquiry");
        return new WorkflowReplayEvidenceView(
                workflowEvidenceSummaryParser.tokenOrDefault(businessTokens, "factStatus", "UNKNOWN"),
                workflowEvidenceSummaryParser.summarizeBusinessFacts(subIntent, businessFactsSummary),
                workflowEvidenceSummaryParser.splitPipeList(businessTokens.get("sourceSystems")),
                workflowEvidenceSummaryParser.parseInt(businessTokens.get("factCount"), 0),
                workflowEvidenceSummaryParser.parseInt(businessTokens.get("missingEntityCount"), 0),
                workflowEvidenceSummaryParser.parseInt(businessTokens.get("conflictFlagCount"), 0),
                workflowEvidenceSummaryParser.summarizeKnowledgeRole(subIntent),
                workflowEvidenceSummaryParser.tokenOrDefault(knowledgeTokens, "retrievalSource", "unknown"),
                workflowEvidenceSummaryParser.parseInt(knowledgeTokens.get("knowledgeRecallCount"), 0),
                workflowEvidenceSummaryParser.splitPipeList(knowledgeTokens.get("snippetIds")),
                workflowEvidenceSummaryParser.tokenOrDefault(replyTokens, "replySource", "UNKNOWN").toUpperCase(java.util.Locale.ROOT),
                replyTokens.get("fallbackReason")
        );
    }

    private Optional<String> findEventSummary(List<WorkflowEvent> events, com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage stage) {
        return events.stream()
                .filter(event -> event.stage() == stage)
                .reduce((first, second) -> second)
                .map(WorkflowEvent::summary);
    }
}
