package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentRoutingService;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrieveService;
import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
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
                                 WorkflowProperties workflowProperties) {
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
    }

    public WorkflowRun execute(WorkflowRun run, InboundMail inboundMail) {
        try {
            InboundMail cleanedMail = mailCleaner.clean(inboundMail);
            advance(run, WorkflowStage.MAIL_CLEANED, "mail cleaned, body length=" + cleanedMail.rawBody().length());

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

            ReplyDraft draft = replyDraftService.draft(run, cleanedMail, normalizationResult, routeResult, contextSnapshot, businessFactResult, knowledgeRetrieveResult);
            replyDraftRepository.save(draft);
            advance(run, WorkflowStage.REPLY_DRAFTED, "draftStatus=%s".formatted(draft.getStatus()));

            ReviewDecision reviewDecision = reviewDecisionService.review(draft);
            draft.revise(draft.getSubject(), draft.getBody(), reviewDecision.finalStatus(), reviewDecision.reviewReason());
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
}
