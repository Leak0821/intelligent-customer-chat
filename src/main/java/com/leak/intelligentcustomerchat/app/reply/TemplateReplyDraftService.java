package com.leak.intelligentcustomerchat.app.reply;

import com.leak.intelligentcustomerchat.app.ai.LlmClient;
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
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import com.leak.intelligentcustomerchat.domain.workflow.WorkflowRun;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TemplateReplyDraftService implements ReplyDraftService {
    private final PromptConfigService promptConfigService;
    private final LlmClient llmClient;

    public TemplateReplyDraftService(PromptConfigService promptConfigService, LlmClient llmClient) {
        this.promptConfigService = promptConfigService;
        this.llmClient = llmClient;
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
        ReplyBodyResult replyBodyResult = buildBody(mail, routeResult, normalizationResult, businessFactResult, knowledgeRetrieveResult, contextSnapshot, status);
        String notes = "scene=%s, subIntent=%s, replySource=%s"
                .formatted(routeResult.scene(), routeResult.subIntent(), replyBodyResult.source());
        return ReplyDraft.create(run.getRunId(), subject, replyBodyResult.body(), status, notes);
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

    private ReplyBodyResult buildBody(InboundMail mail,
                                      IntentRouteResult routeResult,
                                      IntentNormalizationResult normalizationResult,
                                      BusinessFactResult businessFactResult,
                                      KnowledgeRetrieveResult knowledgeRetrieveResult,
                                      ContextSnapshot contextSnapshot,
                                      ReplyDraftStatus status) {
        PromptTemplateConfig promptConfig = promptConfigService.currentPromptConfig();
        if (status == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return new ReplyBodyResult(renderTemplate(promptConfig.followUpTemplate(), normalizationResult.primaryQuestion(), routeResult.scene().name()), "follow-up-template");
        }

        if (status == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return new ReplyBodyResult(renderTemplate(promptConfig.humanReviewTemplate(), normalizationResult.primaryQuestion(), routeResult.scene().name()), "human-review-template");
        }

        String fallbackBody = buildTemplateDirectReply(routeResult, normalizationResult, businessFactResult, knowledgeRetrieveResult, contextSnapshot, promptConfig.directReplySuffix());
        return llmClient.complete(
                        promptConfig.directReplySystemPrompt(),
                        buildDirectReplyPrompt(mail, routeResult, normalizationResult, businessFactResult, knowledgeRetrieveResult, contextSnapshot, promptConfig.directReplySuffix())
                )
                .filter(value -> !value.isBlank())
                .map(body -> new ReplyBodyResult(body, "llm"))
                .orElseGet(() -> new ReplyBodyResult(fallbackBody, "template"));
    }

    private String buildDirectReplyPrompt(InboundMail mail,
                                          IntentRouteResult routeResult,
                                          IntentNormalizationResult normalizationResult,
                                          BusinessFactResult businessFactResult,
                                          KnowledgeRetrieveResult knowledgeRetrieveResult,
                                          ContextSnapshot contextSnapshot,
                                          String directReplySuffix) {
        return """
                Customer email subject:
                %s

                Customer email body:
                %s

                Normalized request:
                %s

                Primary question:
                %s

                Routed scene and intent:
                - scene=%s
                - subIntent=%s

                Context summary:
                %s

                Business facts:
                %s

                Knowledge snippets:
                %s

                Closing instruction:
                %s
                """.formatted(
                mail.subject(),
                mail.rawBody(),
                normalizationResult.normalizedRequest(),
                normalizationResult.primaryQuestion(),
                routeResult.scene(),
                routeResult.subIntent(),
                contextSnapshot.threadSummary(),
                renderFacts(businessFactResult),
                renderKnowledgeSnippets(knowledgeRetrieveResult),
                directReplySuffix
        );
    }

    private String buildTemplateDirectReply(IntentRouteResult routeResult,
                                            IntentNormalizationResult normalizationResult,
                                            BusinessFactResult businessFactResult,
                                            KnowledgeRetrieveResult knowledgeRetrieveResult,
                                            ContextSnapshot contextSnapshot,
                                            String directReplySuffix) {
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

    private String renderFacts(BusinessFactResult businessFactResult) {
        if (businessFactResult.facts().isEmpty()) {
            return "[]";
        }
        return businessFactResult.facts().stream()
                .collect(Collectors.joining(" | ", "[", "]"));
    }

    private String renderKnowledgeSnippets(KnowledgeRetrieveResult knowledgeRetrieveResult) {
        if (knowledgeRetrieveResult.snippets().isEmpty()) {
            return "[]";
        }
        return knowledgeRetrieveResult.snippets().stream()
                .map(KnowledgeSnippet::content)
                .collect(Collectors.joining(" | ", "[", "]"));
    }

    private record ReplyBodyResult(String body, String source) {
    }
}
