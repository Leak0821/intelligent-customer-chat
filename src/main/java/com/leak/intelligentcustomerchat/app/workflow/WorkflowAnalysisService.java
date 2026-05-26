package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.app.business.BusinessFactService;
import com.leak.intelligentcustomerchat.app.context.ContextLoadingService;
import com.leak.intelligentcustomerchat.app.intent.IntentNormalizationService;
import com.leak.intelligentcustomerchat.app.intent.IntentRoutingService;
import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeRetrieveService;
import com.leak.intelligentcustomerchat.app.mail.MailCleaner;
import com.leak.intelligentcustomerchat.app.reply.ReplyDraftService;
import com.leak.intelligentcustomerchat.app.review.ReviewDecisionService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.springframework.stereotype.Service;

@Service
public class WorkflowAnalysisService {
    private final MailCleaner mailCleaner;
    private final IntentNormalizationService intentNormalizationService;
    private final IntentRoutingService intentRoutingService;
    private final ContextLoadingService contextLoadingService;
    private final BusinessFactService businessFactService;
    private final KnowledgeRetrieveService knowledgeRetrieveService;
    private final ReplyDraftService replyDraftService;
    private final ReviewDecisionService reviewDecisionService;

    public WorkflowAnalysisService(MailCleaner mailCleaner,
                                   IntentNormalizationService intentNormalizationService,
                                   IntentRoutingService intentRoutingService,
                                   ContextLoadingService contextLoadingService,
                                   BusinessFactService businessFactService,
                                   KnowledgeRetrieveService knowledgeRetrieveService,
                                   ReplyDraftService replyDraftService,
                                   ReviewDecisionService reviewDecisionService) {
        this.mailCleaner = mailCleaner;
        this.intentNormalizationService = intentNormalizationService;
        this.intentRoutingService = intentRoutingService;
        this.contextLoadingService = contextLoadingService;
        this.businessFactService = businessFactService;
        this.knowledgeRetrieveService = knowledgeRetrieveService;
        this.replyDraftService = replyDraftService;
        this.reviewDecisionService = reviewDecisionService;
    }

    public WorkflowAnalysisView analyze(InboundMail inboundMail) {
        InboundMail cleanedMail = mailCleaner.clean(inboundMail);
        IntentNormalizationResult normalizationResult = intentNormalizationService.normalize(cleanedMail);
        IntentRouteResult routeResult = intentRoutingService.route(normalizationResult);
        ContextSnapshot contextSnapshot = contextLoadingService.load(cleanedMail, routeResult);
        BusinessFactResult businessFactResult = businessFactService.loadFacts(cleanedMail, normalizationResult, routeResult, contextSnapshot);
        KnowledgeRetrieveResult knowledgeRetrieveResult = knowledgeRetrieveService.retrieve(normalizationResult, routeResult, businessFactResult);

        // 分析视图不落正式状态机，只复用同一套草稿与审核能力，方便快速检查中间产物。
        WorkflowRun previewRun = WorkflowRun.start(cleanedMail.messageId(), cleanedMail.threadId());
        ReplyDraft draft = replyDraftService.draft(
                previewRun,
                cleanedMail,
                normalizationResult,
                routeResult,
                contextSnapshot,
                businessFactResult,
                knowledgeRetrieveResult
        );
        ReviewDecision reviewDecision = reviewDecisionService.review(draft);

        return new WorkflowAnalysisView(
                cleanedMail,
                normalizationResult,
                routeResult,
                contextSnapshot,
                businessFactResult,
                knowledgeRetrieveResult,
                draft,
                reviewDecision
        );
    }
}
