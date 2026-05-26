package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.springframework.stereotype.Service;

@Service
public class TemplateReplyDraftService implements ReplyDraftService {

    @Override
    public ReplyDraft draft(WorkflowRun run,
                            InboundMail mail,
                            IntentNormalizationResult normalizationResult,
                            IntentRouteResult routeResult,
                            ContextSnapshot contextSnapshot,
                            BusinessFactResult businessFactResult,
                            KnowledgeRetrieveResult knowledgeRetrieveResult) {
        String subject = "Re: " + mail.subject();
        ReplyDraftStatus status = decideStatus(normalizationResult, businessFactResult);
        String body = buildBody(routeResult, normalizationResult, businessFactResult, knowledgeRetrieveResult, contextSnapshot, status);
        String notes = "scene=%s, subIntent=%s".formatted(routeResult.scene(), routeResult.subIntent());
        return ReplyDraft.create(run.getRunId(), subject, body, status, notes);
    }

    private ReplyDraftStatus decideStatus(IntentNormalizationResult normalizationResult, BusinessFactResult businessFactResult) {
        if (normalizationResult.disposition() == ProcessingDisposition.HUMAN_REVIEW) {
            return ReplyDraftStatus.HUMAN_REVIEW_REQUIRED;
        }
        if (normalizationResult.disposition() == ProcessingDisposition.FOLLOW_UP
                || businessFactResult.status() == BusinessFactStatus.INSUFFICIENT_INPUT) {
            return ReplyDraftStatus.FOLLOW_UP_NEEDED;
        }
        return ReplyDraftStatus.DRAFT_READY;
    }

    private String buildBody(IntentRouteResult routeResult,
                             IntentNormalizationResult normalizationResult,
                             BusinessFactResult businessFactResult,
                             KnowledgeRetrieveResult knowledgeRetrieveResult,
                             ContextSnapshot contextSnapshot,
                             ReplyDraftStatus status) {
        if (status == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return """
                    Hello,
                    
                    We have reviewed your latest email. To continue, please share your order number or tracking number so we can verify the exact record for you.
                    
                    Current understanding:
                    - Main request: %s
                    - Routed scene: %s
                    
                    Once we receive the missing information, we will continue with the next step.
                    """
                    .formatted(normalizationResult.primaryQuestion(), routeResult.scene());
        }

        if (status == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return """
                    Hello,
                    
                    We have reviewed your latest request and a specialist is checking the case to avoid giving you an incorrect commitment.
                    
                    Current understanding:
                    - Main request: %s
                    - Routed scene: %s
                    
                    We will follow up again after manual review.
                    """
                    .formatted(normalizationResult.primaryQuestion(), routeResult.scene());
        }

        String routeLine = routeResult.scene() == CustomerScene.PRE_SALES
                ? "We prepared a pre-sales reply direction for your request."
                : "We prepared an after-sales reply direction based on the latest facts we have.";
        return """
                Hello,
                
                %s
                
                Current understanding:
                - Main request: %s
                - Route result: %s / %s
                - Business facts: %s
                - Knowledge snippets: %s
                - Context summary: %s
                
                This draft is still running on the first production skeleton and will be refined with live tools, live facts, and RAG evidence in the next slices.
                """
                .formatted(
                        routeLine,
                        normalizationResult.primaryQuestion(),
                        routeResult.scene(),
                        routeResult.subIntent(),
                        businessFactResult.facts(),
                        knowledgeRetrieveResult.snippets(),
                        contextSnapshot.threadSummary()
                );
    }
}
