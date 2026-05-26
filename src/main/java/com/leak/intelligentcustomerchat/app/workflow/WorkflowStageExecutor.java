package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentRoutingService;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrieveService;
import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftingResult;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionContext;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionService;
import com.leak.intelligentcustomerchat.config.WorkflowProperties;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftRepository;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.reply.SendReadiness;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowEvent;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRunRepository;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowStage;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStageExecutor {
    private final MailCleaner mailCleaner;
    private final IntentNormalizationService intentNormalizationService;
    private final IntentRoutingService intentRoutingService;
    private final ContextLoadingService contextLoadingService;
    private final BusinessFactService businessFactService;
    private final KnowledgeRetrieveService knowledgeRetrieveService;
    private final ReplyDraftService replyDraftService;
    private final ReviewDecisionService reviewDecisionService;
    private final ReplyDraftRepository replyDraftRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowEventRecorder workflowEventRecorder;
    private final WorkflowProperties workflowProperties;
    private final WorkflowDemoFaultService workflowDemoFaultService;

    public WorkflowStageExecutor(MailCleaner mailCleaner,
                                 IntentNormalizationService intentNormalizationService,
                                 IntentRoutingService intentRoutingService,
                                 ContextLoadingService contextLoadingService,
                                 BusinessFactService businessFactService,
                                 KnowledgeRetrieveService knowledgeRetrieveService,
                                 ReplyDraftService replyDraftService,
                                 ReviewDecisionService reviewDecisionService,
                                 ReplyDraftRepository replyDraftRepository,
                                 WorkflowRunRepository workflowRunRepository,
                                 WorkflowEventRecorder workflowEventRecorder,
                                 WorkflowProperties workflowProperties,
                                 WorkflowDemoFaultService workflowDemoFaultService) {
        this.mailCleaner = mailCleaner;
        this.intentNormalizationService = intentNormalizationService;
        this.intentRoutingService = intentRoutingService;
        this.contextLoadingService = contextLoadingService;
        this.businessFactService = businessFactService;
        this.knowledgeRetrieveService = knowledgeRetrieveService;
        this.replyDraftService = replyDraftService;
        this.reviewDecisionService = reviewDecisionService;
        this.replyDraftRepository = replyDraftRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.workflowEventRecorder = workflowEventRecorder;
        this.workflowProperties = workflowProperties;
        this.workflowDemoFaultService = workflowDemoFaultService;
    }

    public WorkflowRun execute(WorkflowRun run, InboundMail inboundMail) {
        try {
            InboundMail cleanedMail = mailCleaner.clean(inboundMail);
            advance(run, WorkflowStage.MAIL_CLEANED, "mail cleaned, body length=" + cleanedMail.rawBody().length());
            workflowDemoFaultService.failIfNeeded(cleanedMail);

            // 关键节点先用占位实现串起完整状态机，后续可以逐个替换成真实 AI、RAG 和业务查询能力。
            IntentNormalizationResult normalizationResult = intentNormalizationService.normalize(cleanedMail);
            advance(run, WorkflowStage.INTENT_NORMALIZED,
                    "disposition=%s, missing=%s".formatted(normalizationResult.disposition(), normalizationResult.missingEntities()));

            IntentRouteResult routeResult = intentRoutingService.route(normalizationResult);
            advance(run, WorkflowStage.INTENT_ROUTED,
                    "scene=%s, subIntent=%s".formatted(routeResult.scene(), routeResult.subIntent()));

            ContextSnapshot contextSnapshot = contextLoadingService.load(cleanedMail, routeResult);
            advance(run, WorkflowStage.CONTEXT_LOADED,
                    "strongSignals=%s, optionalSignals=%s".formatted(contextSnapshot.strongSignals().size(), contextSnapshot.optionalSignals().size()));

            BusinessFactResult businessFactResult = businessFactService.loadFacts(cleanedMail, normalizationResult, routeResult, contextSnapshot);
            advance(run, WorkflowStage.BUSINESS_FACTS_READY,
                    "factStatus=%s, facts=%s".formatted(businessFactResult.status(), businessFactResult.facts().size()));

            KnowledgeRetrieveResult knowledgeRetrieveResult = knowledgeRetrieveService.retrieve(normalizationResult, routeResult, businessFactResult);
            advance(run, WorkflowStage.KNOWLEDGE_READY,
                    "knowledgeRecallCount=%s".formatted(knowledgeRetrieveResult.recallCount()));

            ReplyDraftingResult draftingResult = replyDraftService.draftWithDiagnostics(
                    run,
                    cleanedMail,
                    normalizationResult,
                    routeResult,
                    contextSnapshot,
                    businessFactResult,
                    knowledgeRetrieveResult
            );
            ReplyDraft draft = draftingResult.draft();
            replyDraftRepository.save(draft);
            advance(run, WorkflowStage.REPLY_DRAFTED, buildDraftStageReason(draftingResult));

            ReviewDecision reviewDecision = reviewDecisionService.review(
                    draft,
                    new ReviewDecisionContext(routeResult, businessFactResult, knowledgeRetrieveResult)
            );
            draft.applyReviewOutcome(reviewDecision.finalStatus(), reviewDecision.reviewReason());
            draft.updateSendReadiness(
                    deriveSendReadiness(reviewDecision),
                    deriveNextAction(reviewDecision),
                    reviewDecision.reviewReason()
            );
            replyDraftRepository.save(draft);
            advance(run, WorkflowStage.REVIEWED, reviewDecision.reviewReason());

            if (workflowProperties.autoCompleteEmptyChain()) {
                run.complete("workflow completed with draft status=" + draft.getStatus());
                workflowRunRepository.save(run);
                workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
            }
            return run;
        } catch (RuntimeException ex) {
            // 这里统一把异常折叠为工作流事件，后续接入告警、重试和死信队列时仍然沿用这个出口。
            run.block("slice-1 workflow blocked: " + ex.getMessage());
            workflowRunRepository.save(run);
            workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
            return run;
        }
    }

    private void advance(WorkflowRun run, WorkflowStage stage, String reason) {
        run.moveTo(stage, reason);
        workflowRunRepository.save(run);
        workflowEventRecorder.record(WorkflowEvent.fromRun(run, run.getStatusReason()));
    }

    private SendReadiness deriveSendReadiness(ReviewDecision reviewDecision) {
        if (reviewDecision.finalStatus() == ReplyDraftStatus.BLOCKED) {
            return SendReadiness.HOLD;
        }
        if (reviewDecision.finalStatus() == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return SendReadiness.PENDING_REVIEW;
        }
        if (reviewDecision.finalStatus() == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return SendReadiness.NOT_APPLICABLE;
        }
        return reviewDecision.autoSendAllowed() ? SendReadiness.READY_FOR_SEND : SendReadiness.PENDING_REVIEW;
    }

    private String deriveNextAction(ReviewDecision reviewDecision) {
        if (reviewDecision.finalStatus() == ReplyDraftStatus.BLOCKED) {
            return "hold_dispatch";
        }
        if (reviewDecision.finalStatus() == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return "manual_review_required";
        }
        if (reviewDecision.finalStatus() == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return "request_customer_information";
        }
        return reviewDecision.autoSendAllowed() ? "dispatch_reply" : "manual_send_review";
    }

    private String buildDraftStageReason(ReplyDraftingResult draftingResult) {
        String reason = "draftStatus=%s, replySource=%s".formatted(
                draftingResult.draft().getStatus(),
                draftingResult.diagnostics().replySource()
        );
        if (draftingResult.diagnostics().fallbackReason() == null || draftingResult.diagnostics().fallbackReason().isBlank()) {
            return reason;
        }
        return reason + ", fallbackReason=" + draftingResult.diagnostics().fallbackReason();
    }
}
