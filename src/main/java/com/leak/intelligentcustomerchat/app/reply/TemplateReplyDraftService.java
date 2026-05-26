package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.app.config.PromptConfigService;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraftStatus;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TemplateReplyDraftService implements ReplyDraftService {
    private final PromptConfigService promptConfigService;

    public TemplateReplyDraftService(PromptConfigService promptConfigService) {
        this.promptConfigService = promptConfigService;
    }

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
        String followUpTemplate = promptConfigService.currentPromptConfig().followUpTemplate();
        String humanReviewTemplate = promptConfigService.currentPromptConfig().humanReviewTemplate();
        String directReplySuffix = promptConfigService.currentPromptConfig().directReplySuffix();
        if (status == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return renderTemplate(followUpTemplate, normalizationResult.primaryQuestion(), routeResult.scene().name());
        }

        if (status == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return renderTemplate(humanReviewTemplate, normalizationResult.primaryQuestion(), routeResult.scene().name());
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
                
                %s
                """
                .formatted(
                        routeLine,
                        normalizationResult.primaryQuestion(),
                        routeResult.scene(),
                        routeResult.subIntent(),
                        businessFactResult.facts(),
                        renderKnowledgeSnippets(knowledgeRetrieveResult),
                        contextSnapshot.threadSummary(),
                        directReplySuffix
                );
    }

    private String renderTemplate(String template, String primaryQuestion, String scene) {
        return template
                .replace("{{primaryQuestion}}", primaryQuestion)
                .replace("{{scene}}", scene);
    }

    private String renderKnowledgeSnippets(KnowledgeRetrieveResult knowledgeRetrieveResult) {
        if (knowledgeRetrieveResult.snippets().isEmpty()) {
            return "[]";
        }
        return knowledgeRetrieveResult.snippets().stream()
                .map(KnowledgeSnippet::content)
                .collect(Collectors.joining(" | ", "[", "]"));
    }
}
