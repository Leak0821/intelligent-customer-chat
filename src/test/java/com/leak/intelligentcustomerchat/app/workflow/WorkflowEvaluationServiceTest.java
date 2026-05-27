package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.mail.MailReceipt;
import com.leak.intelligentcustomerchat.domain.mail.MailReceiptRepository;
import com.leak.intelligentcustomerchat.domain.reply.DispatchTriggerSource;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatch;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDispatchRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecord;
import com.leak.intelligentcustomerchat.domain.review.ReviewRecordRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEventRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEvaluationServiceTest {
    private final WorkflowEvidenceSummaryParser workflowEvidenceSummaryParser = new WorkflowEvidenceSummaryParser();
    private final com.leak.intelligentcustomerchat.app.review.ReviewFeedbackTagger reviewFeedbackTagger =
            new com.leak.intelligentcustomerchat.app.review.ReviewFeedbackTagger();

    @Test
    void shouldBuildEvaluationSampleFromWorkflowArtifacts() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryWorkflowEventRepository workflowEventRepository = new InMemoryWorkflowEventRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun run = WorkflowRun.start("msg-eval-1", "thread-eval-1");
        run.moveTo(WorkflowStage.INTENT_NORMALIZED, "disposition=CONTINUE, missing=[]");
        run.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=logistics_tracking");
        run.moveTo(WorkflowStage.BUSINESS_FACTS_READY, "factStatus=CONFLICT, sourceSystems=local-order-catalog|local-logistics-catalog, resolvedEntityCount=1, factCount=1, missingEntityCount=0, conflictFlagCount=1");
        run.moveTo(WorkflowStage.KNOWLEDGE_READY, "knowledgeRecallCount=3, retrievalSource=elasticsearch-hybrid, snippetIds=seed-1|seed-2|seed-3");
        run.complete("workflow completed with draft status=HUMAN_REVIEW_REQUIRED");
        workflowRunRepository.save(run);

        workflowEventRepository.save(new WorkflowEvent("evt-1", run.getRunId(), run.getMessageId(), WorkflowStage.INTENT_NORMALIZED, run.getStatus(), "disposition=CONTINUE, missing=[]", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-2", run.getRunId(), run.getMessageId(), WorkflowStage.INTENT_ROUTED, run.getStatus(), "scene=AFTER_SALES, subIntent=logistics_tracking", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-3", run.getRunId(), run.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, run.getStatus(), "factStatus=CONFLICT, sourceSystems=local-order-catalog|local-logistics-catalog, resolvedEntityCount=1, factCount=1, missingEntityCount=0, conflictFlagCount=1", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-4", run.getRunId(), run.getMessageId(), WorkflowStage.KNOWLEDGE_READY, run.getStatus(), "knowledgeRecallCount=3, retrievalSource=elasticsearch-hybrid, snippetIds=seed-1|seed-2|seed-3", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-5", run.getRunId(), run.getMessageId(), WorkflowStage.REPLY_DRAFTED, run.getStatus(), "draftStatus=HUMAN_REVIEW_REQUIRED, replySource=human-review-template, fallbackReason=human_review_template_required", OffsetDateTime.now()));

        ReplyDraft draft = ReplyDraft.create(run.getRunId(), "Re: Where is my order", "Please allow us to check.", ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, "manual review required");
        draft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "manual_review_required", "manual review required");
        replyDraftRepository.save(draft);

        ReplyDispatch dispatch = ReplyDispatch.create(
                run.getRunId(),
                draft.getDraftId(),
                "customer@example.com",
                draft.getSubject(),
                draft.getBody(),
                3,
                DispatchTriggerSource.MANUAL_APPROVAL,
                "auditor-a",
                "approved"
        );
        dispatch.markAttemptResult(false, null, "smtp timeout", OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(5));
        replyDispatchRepository.save(dispatch);

        reviewRecordRepository.save(ReviewRecord.reviseDraft(run.getRunId(), draft.getDraftId(), "editor-a", "soften the compensation promise"));
        reviewRecordRepository.save(ReviewRecord.resubmitReview(run.getRunId(), draft.getDraftId(), "editor-a", "resubmitted after manual revision"));
        reviewRecordRepository.save(ReviewRecord.rejectSend(run.getRunId(), draft.getDraftId(), "auditor-b", "promise wording needs revision"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-eval-1", new InboundMail(
                run.getMessageId(),
                run.getThreadId(),
                "customer@example.com",
                "Where is my order",
                "Please help check my order.",
                OffsetDateTime.now()
        )));

        WorkflowEvaluationService service = new WorkflowEvaluationService(
                workflowRunRepository,
                workflowEventRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository,
                workflowEvidenceSummaryParser,
                reviewFeedbackTagger
        );

        WorkflowEvaluationSampleView sample = service.getSample(run.getRunId());

        assertThat(sample.runId()).isEqualTo(run.getRunId());
        assertThat(sample.subject()).isEqualTo("Where is my order");
        assertThat(sample.routingSummary()).contains("AFTER_SALES");
        assertThat(sample.businessFactsTriggered()).isTrue();
        assertThat(sample.businessFactStatus()).isEqualTo("CONFLICT");
        assertThat(sample.businessFactRole()).contains("authority check");
        assertThat(sample.businessFactSourceSystems()).containsExactly("local-order-catalog", "local-logistics-catalog");
        assertThat(sample.knowledgeTriggered()).isTrue();
        assertThat(sample.knowledgeRole()).contains("expectation setting");
        assertThat(sample.knowledgeRetrievalSource()).isEqualTo("elasticsearch-hybrid");
        assertThat(sample.knowledgeRecallCount()).isEqualTo(3);
        assertThat(sample.knowledgeSnippetIds()).containsExactly("seed-1", "seed-2", "seed-3");
        assertThat(sample.draftStatus()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        assertThat(sample.replySource()).isEqualTo("HUMAN-REVIEW-TEMPLATE");
        assertThat(sample.replyFallbackReason()).isEqualTo("human_review_template_required");
        assertThat(sample.latestDispatchStatus()).isEqualTo("RETRY_PENDING");
        assertThat(sample.latestReviewAction()).isEqualTo("REJECT_SEND");
        assertThat(sample.manualReviewOutcome()).isEqualTo("REJECTED_FOR_REVISION");
        assertThat(sample.latestReviewFeedbackTags()).containsExactly("promise_risk", "tone_risk");
        assertThat(sample.reviewCount()).isEqualTo(3);
        assertThat(sample.revisionCount()).isEqualTo(1);
        assertThat(sample.resubmittedForReview()).isTrue();
        assertThat(sample.reviewActionCounts()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("REJECT_SEND", 1),
                new WorkflowEvaluationCountView("RESUBMIT_REVIEW", 1),
                new WorkflowEvaluationCountView("REVISE_DRAFT", 1)
        );
        assertThat(sample.reviewFeedbackTagCounts()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("other", 1),
                new WorkflowEvaluationCountView("promise_risk", 2),
                new WorkflowEvaluationCountView("tone_risk", 2)
        );
        assertThat(sample.reviewTimeline()).contains(
                "REVISE_DRAFT by editor-a: soften the compensation promise",
                "RESUBMIT_REVIEW by editor-a: resubmitted after manual revision",
                "REJECT_SEND by auditor-b: promise wording needs revision"
        );
        assertThat(sample.riskFlags()).contains(
                "manual_review_required",
                "dispatch_retry_pending",
                "review_rejected",
                "business_fact_conflict",
                "reply_human_review_template",
                "reply_fallback_recorded",
                "draft_revised",
                "review_resubmitted"
        );
        assertThat(sample.riskDecision().riskLevel()).isEqualTo("HIGH");
        assertThat(sample.riskDecision().releaseDecision()).isEqualTo("RETRY_PENDING");
        assertThat(sample.riskDecision().sendAllowed()).isFalse();
        assertThat(sample.riskDecision().recommendedAction()).isEqualTo("manual_review_required");
        assertThat(sample.riskDecision().blockingReasons()).contains("dispatch_retry_pending", "review_rejected");
        assertThat(sample.riskDecision().decisionSignals()).anyMatch(item -> item.equals("dispatch_status=RETRY_PENDING"));
        assertThat(sample.scene()).isEqualTo("AFTER_SALES");
        assertThat(sample.subIntent()).isEqualTo("LOGISTICS_TRACKING");
    }

    @Test
    void shouldFilterSamplesBySceneStatusAndRiskFlag() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryWorkflowEventRepository workflowEventRepository = new InMemoryWorkflowEventRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun afterSalesRun = WorkflowRun.start("msg-eval-2", "thread-eval-2");
        afterSalesRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=logistics_tracking");
        afterSalesRun.complete("workflow completed with draft status=FOLLOW_UP_NEEDED");
        workflowRunRepository.save(afterSalesRun);
        workflowEventRepository.save(new WorkflowEvent("evt-a", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.INTENT_ROUTED, afterSalesRun.getStatus(), "scene=AFTER_SALES, subIntent=logistics_tracking", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-a1", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, afterSalesRun.getStatus(), "factStatus=NO_RESULT, sourceSystems=local-order-catalog|local-logistics-catalog, resolvedEntityCount=1, factCount=0, missingEntityCount=0, conflictFlagCount=0", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-a1b", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.KNOWLEDGE_READY, afterSalesRun.getStatus(), "knowledgeRecallCount=2, retrievalSource=elasticsearch-hybrid, snippetIds=seed-a1|seed-a2", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-a2", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.REPLY_DRAFTED, afterSalesRun.getStatus(), "draftStatus=FOLLOW_UP_NEEDED, replySource=follow-up-template, fallbackReason=follow_up_template_required", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(afterSalesRun.getRunId(), "subject-a", "body-a", ReplyDraftStatus.FOLLOW_UP_NEEDED, "missing order id"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-a", new InboundMail(afterSalesRun.getMessageId(), afterSalesRun.getThreadId(), "a@example.com", "subject-a", "body-a", OffsetDateTime.now())));

        WorkflowRun preSalesRun = WorkflowRun.start("msg-eval-3", "thread-eval-3");
        preSalesRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=PRE_SALES, subIntent=product_recommendation");
        preSalesRun.complete("workflow completed with draft status=DRAFT_READY");
        workflowRunRepository.save(preSalesRun);
        workflowEventRepository.save(new WorkflowEvent("evt-b", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.INTENT_ROUTED, preSalesRun.getStatus(), "scene=PRE_SALES, subIntent=product_recommendation", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-b1", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, preSalesRun.getStatus(), "factStatus=NOT_REQUIRED, sourceSystems=none, resolvedEntityCount=0, factCount=0, missingEntityCount=0, conflictFlagCount=0", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-b1b", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.KNOWLEDGE_READY, preSalesRun.getStatus(), "knowledgeRecallCount=3, retrievalSource=catalog-search, snippetIds=seed-b1|seed-b2|seed-b3", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-b2", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.REPLY_DRAFTED, preSalesRun.getStatus(), "draftStatus=DRAFT_READY, replySource=template, fallbackReason=llm_unavailable_or_empty", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(preSalesRun.getRunId(), "subject-b", "body-b", ReplyDraftStatus.DRAFT_READY, "ready"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-b", new InboundMail(preSalesRun.getMessageId(), preSalesRun.getThreadId(), "b@example.com", "subject-b", "body-b", OffsetDateTime.now())));

        WorkflowEvaluationService service = new WorkflowEvaluationService(
                workflowRunRepository,
                workflowEventRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository,
                workflowEvidenceSummaryParser,
                reviewFeedbackTagger
        );

        List<WorkflowEvaluationSampleView> afterSalesSamples = service.listSamples(10, "AFTER_SALES", null, null, null, null);
        List<WorkflowEvaluationSampleView> followUpSamples = service.listSamples(10, null, null, null, "FOLLOW_UP_NEEDED", "follow_up_needed");
        List<WorkflowEvaluationSampleView> templateFallbackSamples = service.listSamples(10, null, null, null, null, "reply_template_fallback");
        List<WorkflowEvaluationSampleView> noResultFactSamples = service.listSamples(10, null, null, null, null, null, "NO_RESULT", null, null);
        List<WorkflowEvaluationSampleView> hybridKnowledgeSamples = service.listSamples(10, null, null, null, null, null, null, "elasticsearch-hybrid", null);
        List<WorkflowEvaluationSampleView> llmFallbackSamples = service.listSamples(10, null, null, null, null, null, null, null, "llm_unavailable_or_empty");
        List<WorkflowEvaluationSampleView> factRoleSamples = service.listSamples(10, null, null, null, null, null, null, "business facts were queried but did not return a usable record", null, null, null);
        List<WorkflowEvaluationSampleView> policyKnowledgeRoleSamples = service.listSamples(10, null, null, null, null, null, null, null, "knowledge fills product and catalog guidance that business facts do not provide", null, null);

        assertThat(afterSalesSamples).hasSize(1);
        assertThat(afterSalesSamples.get(0).messageId()).isEqualTo("msg-eval-2");
        assertThat(afterSalesSamples.get(0).replySource()).isEqualTo("FOLLOW-UP-TEMPLATE");
        assertThat(followUpSamples).hasSize(1);
        assertThat(followUpSamples.get(0).scene()).isEqualTo("AFTER_SALES");
        assertThat(templateFallbackSamples).hasSize(1);
        assertThat(templateFallbackSamples.get(0).messageId()).isEqualTo("msg-eval-3");
        assertThat(templateFallbackSamples.get(0).replyFallbackReason()).isEqualTo("llm_unavailable_or_empty");
        assertThat(noResultFactSamples).hasSize(1);
        assertThat(noResultFactSamples.get(0).businessFactStatus()).isEqualTo("NO_RESULT");
        assertThat(hybridKnowledgeSamples).hasSize(1);
        assertThat(hybridKnowledgeSamples.get(0).knowledgeRetrievalSource()).isEqualTo("elasticsearch-hybrid");
        assertThat(llmFallbackSamples).hasSize(1);
        assertThat(llmFallbackSamples.get(0).replyFallbackReason()).isEqualTo("llm_unavailable_or_empty");
        assertThat(factRoleSamples).hasSize(1);
        assertThat(factRoleSamples.get(0).businessFactRole()).contains("did not return a usable record");
        assertThat(policyKnowledgeRoleSamples).hasSize(1);
        assertThat(policyKnowledgeRoleSamples.get(0).knowledgeRole()).contains("product and catalog guidance");
    }

    @Test
    void shouldSummarizeRecentSamplesForDemoObservation() {
        InMemoryWorkflowRunRepository workflowRunRepository = new InMemoryWorkflowRunRepository();
        InMemoryWorkflowEventRepository workflowEventRepository = new InMemoryWorkflowEventRepository();
        InMemoryReplyDraftRepository replyDraftRepository = new InMemoryReplyDraftRepository();
        InMemoryReplyDispatchRepository replyDispatchRepository = new InMemoryReplyDispatchRepository();
        InMemoryReviewRecordRepository reviewRecordRepository = new InMemoryReviewRecordRepository();
        InMemoryMailReceiptRepository mailReceiptRepository = new InMemoryMailReceiptRepository();

        WorkflowRun afterSalesRun = WorkflowRun.start("msg-summary-1", "thread-summary-1");
        afterSalesRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=logistics_tracking");
        afterSalesRun.complete("workflow completed with draft status=FOLLOW_UP_NEEDED");
        workflowRunRepository.save(afterSalesRun);
        workflowEventRepository.save(new WorkflowEvent("evt-s1", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.INTENT_ROUTED, afterSalesRun.getStatus(), "scene=AFTER_SALES, subIntent=logistics_tracking", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s1a", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, afterSalesRun.getStatus(), "factStatus=INSUFFICIENT_INPUT, sourceSystems=local-order-catalog|local-logistics-catalog, resolvedEntityCount=0, factCount=0, missingEntityCount=2, conflictFlagCount=0", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s1b", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.KNOWLEDGE_READY, afterSalesRun.getStatus(), "knowledgeRecallCount=2, retrievalSource=elasticsearch-hybrid, snippetIds=seed-s1|seed-s2", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s2", afterSalesRun.getRunId(), afterSalesRun.getMessageId(), WorkflowStage.REPLY_DRAFTED, afterSalesRun.getStatus(), "draftStatus=FOLLOW_UP_NEEDED, replySource=follow-up-template, fallbackReason=follow_up_template_required", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(afterSalesRun.getRunId(), "subject-s1", "body-s1", ReplyDraftStatus.FOLLOW_UP_NEEDED, "missing order id"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-s1", new InboundMail(afterSalesRun.getMessageId(), afterSalesRun.getThreadId(), "s1@example.com", "subject-s1", "body-s1", OffsetDateTime.now())));

        WorkflowRun preSalesRun = WorkflowRun.start("msg-summary-2", "thread-summary-2");
        preSalesRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=PRE_SALES, subIntent=product_recommendation");
        preSalesRun.complete("workflow completed with draft status=DRAFT_READY");
        workflowRunRepository.save(preSalesRun);
        workflowEventRepository.save(new WorkflowEvent("evt-s3", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.INTENT_ROUTED, preSalesRun.getStatus(), "scene=PRE_SALES, subIntent=product_recommendation", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s3a", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, preSalesRun.getStatus(), "factStatus=NOT_REQUIRED, sourceSystems=none, resolvedEntityCount=0, factCount=0, missingEntityCount=0, conflictFlagCount=0", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s3b", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.KNOWLEDGE_READY, preSalesRun.getStatus(), "knowledgeRecallCount=3, retrievalSource=elasticsearch-hybrid, snippetIds=seed-p1|seed-p2|seed-p3", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s4", preSalesRun.getRunId(), preSalesRun.getMessageId(), WorkflowStage.REPLY_DRAFTED, preSalesRun.getStatus(), "draftStatus=DRAFT_READY, replySource=template, fallbackReason=llm_unavailable_or_empty", OffsetDateTime.now()));
        replyDraftRepository.save(ReplyDraft.create(preSalesRun.getRunId(), "subject-s2", "body-s2", ReplyDraftStatus.DRAFT_READY, "ready"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-s2", new InboundMail(preSalesRun.getMessageId(), preSalesRun.getThreadId(), "s2@example.com", "subject-s2", "body-s2", OffsetDateTime.now())));

        WorkflowRun reviewRun = WorkflowRun.start("msg-summary-3", "thread-summary-3");
        reviewRun.moveTo(WorkflowStage.INTENT_ROUTED, "scene=AFTER_SALES, subIntent=return_refund");
        reviewRun.complete("workflow completed with draft status=HUMAN_REVIEW_REQUIRED");
        workflowRunRepository.save(reviewRun);
        workflowEventRepository.save(new WorkflowEvent("evt-s5", reviewRun.getRunId(), reviewRun.getMessageId(), WorkflowStage.INTENT_ROUTED, reviewRun.getStatus(), "scene=AFTER_SALES, subIntent=return_refund", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s5a", reviewRun.getRunId(), reviewRun.getMessageId(), WorkflowStage.BUSINESS_FACTS_READY, reviewRun.getStatus(), "factStatus=NO_RESULT, sourceSystems=local-order-catalog, resolvedEntityCount=1, factCount=0, missingEntityCount=0, conflictFlagCount=0", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s5b", reviewRun.getRunId(), reviewRun.getMessageId(), WorkflowStage.KNOWLEDGE_READY, reviewRun.getStatus(), "knowledgeRecallCount=2, retrievalSource=policy-catalog, snippetIds=seed-r1|seed-r2", OffsetDateTime.now()));
        workflowEventRepository.save(new WorkflowEvent("evt-s6", reviewRun.getRunId(), reviewRun.getMessageId(), WorkflowStage.REPLY_DRAFTED, reviewRun.getStatus(), "draftStatus=HUMAN_REVIEW_REQUIRED, replySource=human-review-template, fallbackReason=human_review_template_required", OffsetDateTime.now()));
        ReplyDraft reviewDraft = ReplyDraft.create(reviewRun.getRunId(), "subject-s3", "body-s3", ReplyDraftStatus.HUMAN_REVIEW_REQUIRED, "manual review required");
        reviewDraft.updateSendReadiness(SendReadiness.PENDING_REVIEW, "manual_review_required", "manual review required");
        replyDraftRepository.save(reviewDraft);
        reviewRecordRepository.save(ReviewRecord.rejectSend(reviewRun.getRunId(), reviewDraft.getDraftId(), "auditor-summary", "needs stricter wording"));
        mailReceiptRepository.save(MailReceipt.manual("receipt-s3", new InboundMail(reviewRun.getMessageId(), reviewRun.getThreadId(), "s3@example.com", "subject-s3", "body-s3", OffsetDateTime.now())));

        WorkflowEvaluationService service = new WorkflowEvaluationService(
                workflowRunRepository,
                workflowEventRepository,
                replyDraftRepository,
                replyDispatchRepository,
                reviewRecordRepository,
                mailReceiptRepository,
                workflowEvidenceSummaryParser,
                reviewFeedbackTagger
        );

        WorkflowEvaluationSummaryView summary = service.summarizeRecentSamples(10, null, null, null, null, null);
        WorkflowEvaluationSummaryView filteredSummary = service.summarizeRecentSamples(10, "AFTER_SALES", null, null, null, null);
        WorkflowEvaluationSummaryView filteredByFactStatus = service.summarizeRecentSamples(10, null, null, null, null, null, "NO_RESULT", null, null);
        WorkflowEvaluationSummaryView filteredByKnowledgeSource = service.summarizeRecentSamples(10, null, null, null, null, null, null, "policy-catalog", null);
        WorkflowEvaluationSummaryView filteredByFallbackReason = service.summarizeRecentSamples(10, null, null, null, null, null, null, null, "human_review_template_required");
        WorkflowEvaluationSummaryView filteredByFactRole = service.summarizeRecentSamples(10, null, null, null, null, null, null, "business facts were queried but did not return a usable record", null, null, null);
        WorkflowEvaluationSummaryView filteredByKnowledgeRole = service.summarizeRecentSamples(10, null, null, null, null, null, null, null, "knowledge supplements explanation and expectation setting around the current business facts", null, null);

        assertThat(summary.requestedLimit()).isEqualTo(10);
        assertThat(summary.sampledCount()).isEqualTo(3);
        assertThat(summary.scenes()).containsExactly(
                new WorkflowEvaluationCountView("AFTER_SALES", 2),
                new WorkflowEvaluationCountView("PRE_SALES", 1)
        );
        assertThat(summary.subIntents()).containsExactly(
                new WorkflowEvaluationCountView("LOGISTICS_TRACKING", 1),
                new WorkflowEvaluationCountView("PRODUCT_RECOMMENDATION", 1),
                new WorkflowEvaluationCountView("RETURN_REFUND", 1)
        );
        assertThat(summary.draftStatuses()).containsExactly(
                new WorkflowEvaluationCountView("DRAFT_READY", 1),
                new WorkflowEvaluationCountView("FOLLOW_UP_NEEDED", 1),
                new WorkflowEvaluationCountView("HUMAN_REVIEW_REQUIRED", 1)
        );
        assertThat(summary.replySources()).containsExactly(
                new WorkflowEvaluationCountView("FOLLOW-UP-TEMPLATE", 1),
                new WorkflowEvaluationCountView("HUMAN-REVIEW-TEMPLATE", 1),
                new WorkflowEvaluationCountView("TEMPLATE", 1)
        );
        assertThat(summary.businessFactStatuses()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("INSUFFICIENT_INPUT", 1),
                new WorkflowEvaluationCountView("NO_RESULT", 1),
                new WorkflowEvaluationCountView("NOT_REQUIRED", 1)
        );
        assertThat(summary.knowledgeRoles()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("knowledge fills product and catalog guidance that business facts do not provide", 1),
                new WorkflowEvaluationCountView("knowledge supplements policy wording and handling guidance after business facts are checked", 1),
                new WorkflowEvaluationCountView("knowledge supplements explanation and expectation setting around the current business facts", 1)
        );
        assertThat(summary.knowledgeRetrievalSources()).containsExactly(
                new WorkflowEvaluationCountView("elasticsearch-hybrid", 2),
                new WorkflowEvaluationCountView("policy-catalog", 1)
        );
        assertThat(summary.replyFallbackReasons()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("follow_up_template_required", 1),
                new WorkflowEvaluationCountView("human_review_template_required", 1),
                new WorkflowEvaluationCountView("llm_unavailable_or_empty", 1)
        );
        assertThat(summary.latestReviewActions()).containsExactly(
                new WorkflowEvaluationCountView("UNKNOWN", 2),
                new WorkflowEvaluationCountView("REJECT_SEND", 1)
        );
        assertThat(summary.manualReviewOutcomes()).containsExactly(
                new WorkflowEvaluationCountView("NOT_REVIEWED", 2),
                new WorkflowEvaluationCountView("REJECTED_FOR_REVISION", 1)
        );
        assertThat(summary.reviewFeedbackTags()).containsExactly(
                new WorkflowEvaluationCountView("tone_risk", 1)
        );
        assertThat(summary.riskFlags()).contains(
                new WorkflowEvaluationCountView("follow_up_needed", 1),
                new WorkflowEvaluationCountView("manual_review_required", 1),
                new WorkflowEvaluationCountView("reply_template_fallback", 1),
                new WorkflowEvaluationCountView("reply_fallback_recorded", 3)
        );
        assertThat(summary.riskLevels()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("LOW", 1),
                new WorkflowEvaluationCountView("MEDIUM", 1),
                new WorkflowEvaluationCountView("HIGH", 1)
        );
        assertThat(summary.releaseDecisions()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("HOLD_FOR_REVIEW", 2),
                new WorkflowEvaluationCountView("NEED_CUSTOMER_FOLLOW_UP", 1)
        );
        assertThat(summary.recommendedActions()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("await_review_decision", 1),
                new WorkflowEvaluationCountView("manual_review_required", 1),
                new WorkflowEvaluationCountView("request_customer_information", 1)
        );
        assertThat(filteredSummary.sampledCount()).isEqualTo(2);
        assertThat(filteredSummary.scenes()).containsExactly(new WorkflowEvaluationCountView("AFTER_SALES", 2));
        assertThat(filteredSummary.businessFactStatuses()).containsExactlyInAnyOrder(
                new WorkflowEvaluationCountView("INSUFFICIENT_INPUT", 1),
                new WorkflowEvaluationCountView("NO_RESULT", 1)
        );
        assertThat(filteredByFactStatus.sampledCount()).isEqualTo(1);
        assertThat(filteredByFactStatus.businessFactStatuses()).containsExactly(new WorkflowEvaluationCountView("NO_RESULT", 1));
        assertThat(filteredByKnowledgeSource.sampledCount()).isEqualTo(1);
        assertThat(filteredByKnowledgeSource.knowledgeRetrievalSources()).containsExactly(new WorkflowEvaluationCountView("policy-catalog", 1));
        assertThat(filteredByFallbackReason.sampledCount()).isEqualTo(1);
        assertThat(filteredByFallbackReason.replyFallbackReasons()).containsExactly(new WorkflowEvaluationCountView("human_review_template_required", 1));
        assertThat(filteredByFactRole.sampledCount()).isEqualTo(1);
        assertThat(filteredByFactRole.businessFactRoles()).containsExactly(new WorkflowEvaluationCountView("business facts were queried but did not return a usable record", 1));
        assertThat(filteredByKnowledgeRole.sampledCount()).isEqualTo(1);
        assertThat(filteredByKnowledgeRole.knowledgeRoles()).containsExactly(new WorkflowEvaluationCountView("knowledge supplements explanation and expectation setting around the current business facts", 1));
    }

    private static final class InMemoryWorkflowRunRepository implements WorkflowRunRepository {
        private final Map<String, WorkflowRun> runs = new LinkedHashMap<>();

        @Override
        public WorkflowRun save(WorkflowRun run) {
            runs.put(run.getRunId(), run);
            return run;
        }

        @Override
        public Optional<WorkflowRun> findByRunId(String runId) {
            return Optional.ofNullable(runs.get(runId));
        }

        @Override
        public Optional<WorkflowRun> findLatestByMessageId(String messageId) {
            return runs.values().stream().filter(run -> run.getMessageId().equals(messageId)).findFirst();
        }

        @Override
        public List<WorkflowRun> findAll() {
            return runs.values().stream()
                    .sorted(Comparator.comparing(WorkflowRun::getCreatedAt).reversed())
                    .toList();
        }
    }

    private static final class InMemoryWorkflowEventRepository implements WorkflowEventRepository {
        private final List<WorkflowEvent> events = new ArrayList<>();

        @Override
        public void save(WorkflowEvent event) {
            events.add(event);
        }

        @Override
        public List<WorkflowEvent> findByRunId(String runId) {
            return events.stream().filter(event -> event.runId().equals(runId)).toList();
        }

        @Override
        public List<WorkflowEvent> findAll() {
            return List.copyOf(events);
        }
    }

    private static final class InMemoryReplyDraftRepository implements ReplyDraftRepository {
        private final Map<String, ReplyDraft> draftsByRunId = new LinkedHashMap<>();

        @Override
        public ReplyDraft save(ReplyDraft draft) {
            draftsByRunId.put(draft.getRunId(), draft);
            return draft;
        }

        @Override
        public Optional<ReplyDraft> findByRunId(String runId) {
            return Optional.ofNullable(draftsByRunId.get(runId));
        }
    }

    private static final class InMemoryReplyDispatchRepository implements ReplyDispatchRepository {
        private final Map<String, List<ReplyDispatch>> dispatchesByRunId = new LinkedHashMap<>();

        @Override
        public ReplyDispatch save(ReplyDispatch dispatch) {
            dispatchesByRunId.computeIfAbsent(dispatch.getRunId(), key -> new ArrayList<>()).add(dispatch);
            return dispatch;
        }

        @Override
        public List<ReplyDispatch> findByRunId(String runId) {
            return dispatchesByRunId.getOrDefault(runId, List.of());
        }

        @Override
        public Optional<ReplyDispatch> findLatestByRunId(String runId) {
            List<ReplyDispatch> dispatches = dispatchesByRunId.get(runId);
            if (dispatches == null || dispatches.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(dispatches.get(dispatches.size() - 1));
        }

        @Override
        public List<ReplyDispatch> findRetryableDueBefore(OffsetDateTime dueBefore, int limit) {
            return List.of();
        }
    }

    private static final class InMemoryReviewRecordRepository implements ReviewRecordRepository {
        private final List<ReviewRecord> reviewRecords = new ArrayList<>();

        @Override
        public ReviewRecord save(ReviewRecord reviewRecord) {
            reviewRecords.add(reviewRecord);
            return reviewRecord;
        }

        @Override
        public List<ReviewRecord> findByRunId(String runId) {
            return reviewRecords.stream().filter(record -> record.getRunId().equals(runId)).toList();
        }

        @Override
        public Optional<ReviewRecord> findLatestApprovalByRunId(String runId) {
            return reviewRecords.stream()
                    .filter(record -> record.getRunId().equals(runId) && record.isApproval())
                    .reduce((first, second) -> second);
        }
    }

    private static final class InMemoryMailReceiptRepository implements MailReceiptRepository {
        private final Map<String, MailReceipt> receiptsByMessageId = new LinkedHashMap<>();

        @Override
        public boolean existsBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
            return false;
        }

        @Override
        public MailReceipt save(MailReceipt receipt) {
            receiptsByMessageId.put(receipt.getMessageId(), receipt);
            return receipt;
        }

        @Override
        public Optional<MailReceipt> findBySourceFolderAndUid(String sourceKey, String folderName, long uid) {
            return Optional.empty();
        }

        @Override
        public Optional<MailReceipt> findByMessageId(String messageId) {
            return Optional.ofNullable(receiptsByMessageId.get(messageId));
        }

        @Override
        public List<MailReceipt> findPendingForProcessing(int limit) {
            return receiptsByMessageId.values().stream().limit(limit).toList();
        }

        @Override
        public List<MailReceipt> findRecent(int limit) {
            return receiptsByMessageId.values().stream().limit(limit).toList();
        }

        @Override
        public long countAll() {
            return receiptsByMessageId.size();
        }

        @Override
        public long countByStatus(com.leak.intelligentcustomerchat.domain.mail.MailReceiptStatus status) {
            return receiptsByMessageId.values().stream()
                    .filter(receipt -> receipt.getStatus() == status)
                    .count();
        }
    }
}
