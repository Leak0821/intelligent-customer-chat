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

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TemplateReplyDraftService implements ReplyDraftService {
    private static final Pattern STRUCTURED_SIGNAL_PATTERN = Pattern.compile("^[a-z_]+=[^\\s].*$");

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
        return draftWithDiagnostics(run, mail, normalizationResult, routeResult, contextSnapshot, businessFactResult, knowledgeRetrieveResult).draft();
    }

    @Override
    public ReplyDraftingResult draftWithDiagnostics(WorkflowRun run,
                                                    InboundMail mail,
                                                    IntentNormalizationResult normalizationResult,
                                                    IntentRouteResult routeResult,
                                                    ContextSnapshot contextSnapshot,
                                                    BusinessFactResult businessFactResult,
                                                    KnowledgeRetrieveResult knowledgeRetrieveResult) {
        String subject = "Re: " + mail.subject();
        ReplyDraftStatus status = decideStatus(normalizationResult, businessFactResult);
        ContextPromptPayload contextPromptPayload = buildContextPromptPayload(contextSnapshot);
        ReplyCoveragePlan coveragePlan = buildCoveragePlan(normalizationResult);
        ReplyBodyResult replyBodyResult = buildBody(
                mail,
                routeResult,
                normalizationResult,
                businessFactResult,
                knowledgeRetrieveResult,
                contextPromptPayload,
                coveragePlan,
                status
        );
        String notes = "scene=%s, subIntent=%s, replySource=%s"
                .formatted(routeResult.scene(), routeResult.subIntent(), replyBodyResult.source());
        ReplyDraft draft = ReplyDraft.create(run.getRunId(), subject, replyBodyResult.body(), status, notes);
        ReplyDraftingDiagnostics diagnostics = new ReplyDraftingDiagnostics(
                status,
                replyBodyResult.source(),
                replyBodyResult.llmAttempted(),
                replyBodyResult.llmResponseAccepted(),
                replyBodyResult.fallbackReason(),
                coveragePlan.mode(),
                coveragePlan.coveredQuestions(),
                coveragePlan.deferredQuestions(),
                contextPromptPayload.mode(),
                replyBodyResult.systemPrompt(),
                replyBodyResult.userPrompt(),
                contextPromptPayload.preview(),
                contextPromptPayload.strongSignals(),
                List.copyOf(businessFactResult.facts()),
                knowledgeRetrieveResult.snippets().stream().map(KnowledgeSnippet::id).toList(),
                knowledgeRetrieveResult.snippets().stream().map(KnowledgeSnippet::content).toList()
        );
        return new ReplyDraftingResult(draft, diagnostics);
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
                                      ContextPromptPayload contextPromptPayload,
                                      ReplyCoveragePlan coveragePlan,
                                      ReplyDraftStatus status) {
        PromptTemplateConfig promptConfig = promptConfigService.currentPromptConfig();
        if (status == ReplyDraftStatus.FOLLOW_UP_NEEDED) {
            return new ReplyBodyResult(
                    appendCoverageNote(
                            renderTemplate(promptConfig.followUpTemplate(), normalizationResult.primaryQuestion(), routeResult.scene().name()),
                            coveragePlan
                    ),
                    "follow-up-template",
                    false,
                    false,
                    "follow_up_template_required",
                    null,
                    null
            );
        }

        if (status == ReplyDraftStatus.HUMAN_REVIEW_REQUIRED) {
            return new ReplyBodyResult(
                    appendCoverageNote(
                            renderTemplate(promptConfig.humanReviewTemplate(), normalizationResult.primaryQuestion(), routeResult.scene().name()),
                            coveragePlan
                    ),
                    "human-review-template",
                    false,
                    false,
                    "human_review_template_required",
                    null,
                    null
            );
        }

        String systemPrompt = promptConfig.directReplySystemPrompt();
        String userPrompt = buildDirectReplyPrompt(
                mail,
                routeResult,
                normalizationResult,
                businessFactResult,
                knowledgeRetrieveResult,
                contextPromptPayload,
                coveragePlan,
                promptConfig.directReplySuffix()
        );
        String fallbackBody = appendCoverageNote(buildTemplateDirectReply(
                routeResult,
                normalizationResult,
                businessFactResult,
                knowledgeRetrieveResult,
                contextPromptPayload,
                promptConfig.directReplySuffix()
        ), coveragePlan);
        return llmClient.complete(systemPrompt, userPrompt)
                .filter(value -> !value.isBlank())
                .map(body -> new ReplyBodyResult(body, "llm", true, true, null, systemPrompt, userPrompt))
                .orElseGet(() -> new ReplyBodyResult(
                        fallbackBody,
                        "template",
                        true,
                        false,
                        "llm_unavailable_or_empty",
                        systemPrompt,
                        userPrompt
                ));
    }

    private String buildDirectReplyPrompt(InboundMail mail,
                                          IntentRouteResult routeResult,
                                          IntentNormalizationResult normalizationResult,
                                          BusinessFactResult businessFactResult,
                                          KnowledgeRetrieveResult knowledgeRetrieveResult,
                                          ContextPromptPayload contextPromptPayload,
                                          ReplyCoveragePlan coveragePlan,
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

                Reply coverage mode:
                %s

                Covered question(s):
                %s

                Deferred question(s):
                %s

                Context mode:
                %s

                Context strong signals:
                %s

                Context summary:
                %s

                Recent conversation rounds:
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
                coveragePlan.mode(),
                renderList(coveragePlan.coveredQuestions(), "none"),
                renderList(coveragePlan.deferredQuestions(), "none"),
                contextPromptPayload.mode(),
                renderList(contextPromptPayload.strongSignals(), "none"),
                contextPromptPayload.summary(),
                renderList(contextPromptPayload.recentMessages(), "none"),
                renderFacts(businessFactResult),
                renderKnowledgeSnippets(knowledgeRetrieveResult),
                directReplySuffix
        );
    }

    private String buildTemplateDirectReply(IntentRouteResult routeResult,
                                            IntentNormalizationResult normalizationResult,
                                            BusinessFactResult businessFactResult,
                                            KnowledgeRetrieveResult knowledgeRetrieveResult,
                                            ContextPromptPayload contextPromptPayload,
                                            String directReplySuffix) {
        String intentTemplate = buildIntentAwareTemplate(routeResult, normalizationResult, businessFactResult, knowledgeRetrieveResult);
        if (intentTemplate != null) {
            return """
                    Hello,

                    %s

                    Conversation context:
                    - mode: %s
                    - %s

                    %s
                    """.formatted(intentTemplate, contextPromptPayload.mode(), summarizeContextForTemplate(contextPromptPayload), directReplySuffix);
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
                        summarizeContextForTemplate(contextPromptPayload),
                        directReplySuffix
                );
    }

    private String buildIntentAwareTemplate(IntentRouteResult routeResult,
                                            IntentNormalizationResult normalizationResult,
                                            BusinessFactResult businessFactResult,
                                            KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return switch (routeResult.subIntent()) {
            case "logistics_tracking" -> buildLogisticsTrackingTemplate(normalizationResult, businessFactResult, knowledgeRetrieveResult);
            case "order_status" -> buildOrderStatusTemplate(normalizationResult, businessFactResult);
            case "after_sales_policy", "return_refund" -> buildAfterSalesPolicyTemplate(normalizationResult, businessFactResult, knowledgeRetrieveResult);
            case "product_recommendation" -> buildProductRecommendationTemplate(normalizationResult, knowledgeRetrieveResult);
            case "product_comparison" -> buildProductComparisonTemplate(normalizationResult, knowledgeRetrieveResult);
            case "inventory_or_shipping" -> buildInventoryOrShippingTemplate(normalizationResult, knowledgeRetrieveResult);
            default -> null;
        };
    }

    private String buildLogisticsTrackingTemplate(IntentNormalizationResult normalizationResult,
                                                  BusinessFactResult businessFactResult,
                                                  KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return """
                We checked the latest shipping-related information for your request about "%s".

                Current update:
                %s

                Additional guidance:
                %s
                """.formatted(
                normalizationResult.primaryQuestion(),
                renderFactBullets(businessFactResult.facts(), "We have not confirmed a fresh carrier scan yet."),
                renderKnowledgeBullets(knowledgeRetrieveResult, "Tracking events can appear with a short delay depending on the carrier.")
        );
    }

    private String buildOrderStatusTemplate(IntentNormalizationResult normalizationResult,
                                            BusinessFactResult businessFactResult) {
        return """
                We reviewed the latest order information for your request about "%s".

                Current order summary:
                %s

                If you would like, we can continue helping with the next order-related question in the same thread.
                """.formatted(
                normalizationResult.primaryQuestion(),
                renderFactBullets(businessFactResult.facts(), "We have not confirmed a verified order status update yet.")
        );
    }

    private String buildAfterSalesPolicyTemplate(IntentNormalizationResult normalizationResult,
                                                 BusinessFactResult businessFactResult,
                                                 KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return """
                We reviewed the current after-sales guidance related to "%s".

                Policy guidance currently available:
                %s

                We will keep the response aligned with the verified order facts and avoid promising anything unsupported.
                """.formatted(
                normalizationResult.primaryQuestion(),
                renderPolicyOrKnowledge(businessFactResult.facts(), knowledgeRetrieveResult)
        );
    }

    private String buildProductRecommendationTemplate(IntentNormalizationResult normalizationResult,
                                                      KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return """
                We reviewed the request "%s" and prepared a first recommendation direction.

                Recommendation notes:
                %s

                If you want, reply with room size, preferred look, or key features, and we can narrow the options further.
                """.formatted(
                normalizationResult.primaryQuestion(),
                renderKnowledgeBullets(knowledgeRetrieveResult, "We are preparing product suggestions based on the current request.")
        );
    }

    private String buildProductComparisonTemplate(IntentNormalizationResult normalizationResult,
                                                  KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return """
                We reviewed the comparison request "%s".

                Comparison notes:
                %s

                If there is a specific priority such as brightness, control mode, or installation space, we can compare more precisely.
                """.formatted(
                normalizationResult.primaryQuestion(),
                renderKnowledgeBullets(knowledgeRetrieveResult, "We are collecting the available product comparison points.")
        );
    }

    private String buildInventoryOrShippingTemplate(IntentNormalizationResult normalizationResult,
                                                    KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return """
                We reviewed the availability or shipping request "%s".

                Current guidance:
                %s

                If you need a more exact answer, we can continue with the product or destination details in the same thread.
                """.formatted(
                normalizationResult.primaryQuestion(),
                renderKnowledgeBullets(knowledgeRetrieveResult, "We are checking the currently available shipping and availability guidance.")
        );
    }

    private String renderTemplate(String template, String primaryQuestion, String scene) {
        return template
                .replace("{{primaryQuestion}}", primaryQuestion)
                .replace("{{scene}}", scene);
    }

    private ReplyCoveragePlan buildCoveragePlan(IntentNormalizationResult normalizationResult) {
        List<String> coveredQuestions = List.of(normalizationResult.primaryQuestion());
        List<String> deferredQuestions = normalizationResult.secondaryQuestions().stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        String mode = deferredQuestions.isEmpty() ? "primary_only" : "primary_with_deferred_secondary";
        return new ReplyCoveragePlan(mode, coveredQuestions, deferredQuestions);
    }

    private ContextPromptPayload buildContextPromptPayload(ContextSnapshot contextSnapshot) {
        if (contextSnapshot == null) {
            return new ContextPromptPayload("none", "none", List.of(), List.of(), List.of("no context available"));
        }
        List<String> strongSignals = contextSnapshot.strongSignals().stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        List<String> recentMessages = contextSnapshot.optionalSignals().stream()
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> !STRUCTURED_SIGNAL_PATTERN.matcher(value.trim()).matches())
                .limit(3)
                .map(value -> preview(value, 140))
                .toList();
        String summary = normalizeSummary(contextSnapshot.threadSummary());

        String mode;
        if (!"none".equals(summary) && !recentMessages.isEmpty()) {
            mode = "summary_plus_recent";
        } else if (!"none".equals(summary)) {
            mode = "summary_only";
        } else if (!recentMessages.isEmpty()) {
            mode = "recent_messages_only";
        } else if (!strongSignals.isEmpty()) {
            mode = "signals_only";
        } else {
            mode = "none";
        }

        java.util.ArrayList<String> preview = new java.util.ArrayList<>();
        if (!"none".equals(summary)) {
            preview.add("summary=" + summary);
        }
        preview.addAll(recentMessages.stream().map(value -> "recent=" + value).toList());
        preview.addAll(strongSignals.stream().map(value -> "signal=" + value).toList());
        if (preview.isEmpty()) {
            preview.add("no context available");
        }
        return new ContextPromptPayload(mode, summary, recentMessages, strongSignals, List.copyOf(preview));
    }

    private String normalizeSummary(String threadSummary) {
        if (threadSummary == null || threadSummary.isBlank()) {
            return "none";
        }
        String normalized = preview(threadSummary, 180);
        if (normalized.startsWith("thread=") && normalized.contains("latestSubject=")) {
            return "none";
        }
        return normalized;
    }

    private String summarizeContextForTemplate(ContextPromptPayload contextPromptPayload) {
        if (!"none".equals(contextPromptPayload.summary())) {
            return contextPromptPayload.summary();
        }
        if (!contextPromptPayload.recentMessages().isEmpty()) {
            return String.join(" ; ", contextPromptPayload.recentMessages());
        }
        if (!contextPromptPayload.strongSignals().isEmpty()) {
            return String.join(" ; ", contextPromptPayload.strongSignals());
        }
        return "no reusable context";
    }

    private String appendCoverageNote(String body, ReplyCoveragePlan coveragePlan) {
        if (coveragePlan.deferredQuestions().isEmpty()) {
            return body;
        }
        return body + """

                
                Scope note:
                - This reply focuses on the primary question first.
                - Additional questions captured for follow-up if still needed:
                %s
                """.formatted(renderList(coveragePlan.deferredQuestions(), "none"));
    }

    private String renderFacts(BusinessFactResult businessFactResult) {
        if (businessFactResult.facts().isEmpty()) {
            return "[]";
        }
        return businessFactResult.facts().stream()
                .collect(Collectors.joining(" | ", "[", "]"));
    }

    private String renderFactBullets(List<String> facts, String fallback) {
        if (facts.isEmpty()) {
            return "- " + fallback;
        }
        return facts.stream()
                .map(this::toSentenceBullet)
                .collect(Collectors.joining("\n"));
    }

    private String renderKnowledgeBullets(KnowledgeRetrieveResult knowledgeRetrieveResult, String fallback) {
        if (knowledgeRetrieveResult.snippets().isEmpty()) {
            return "- " + fallback;
        }
        return knowledgeRetrieveResult.snippets().stream()
                .map(KnowledgeSnippet::content)
                .map(this::toSentenceBullet)
                .collect(Collectors.joining("\n"));
    }

    private String renderPolicyOrKnowledge(List<String> facts,
                                           KnowledgeRetrieveResult knowledgeRetrieveResult) {
        if (!facts.isEmpty()) {
            return renderFactBullets(facts, "");
        }
        return renderKnowledgeBullets(knowledgeRetrieveResult, "We are using the currently available after-sales guidance.");
    }

    private String renderKnowledgeSnippets(KnowledgeRetrieveResult knowledgeRetrieveResult) {
        if (knowledgeRetrieveResult.snippets().isEmpty()) {
            return "[]";
        }
        return knowledgeRetrieveResult.snippets().stream()
                .map(KnowledgeSnippet::content)
                .collect(Collectors.joining(" | ", "[", "]"));
    }

    private String renderList(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return values.stream()
                .map(value -> "- " + value)
                .collect(Collectors.joining("\n"));
    }

    private String toSentenceBullet(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "- no additional detail";
        }
        normalized = normalized.replace('_', ' ');
        if (!normalized.endsWith(".") && !normalized.endsWith("!") && !normalized.endsWith("?")) {
            normalized = normalized + ".";
        }
        String first = normalized.substring(0, 1).toUpperCase(Locale.ROOT);
        return "- " + first + normalized.substring(1);
    }

    private String preview(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private record ReplyBodyResult(String body,
                                   String source,
                                   boolean llmAttempted,
                                   boolean llmResponseAccepted,
                                   String fallbackReason,
                                   String systemPrompt,
                                   String userPrompt) {
    }

    private record ContextPromptPayload(String mode,
                                        String summary,
                                        List<String> recentMessages,
                                        List<String> strongSignals,
                                        List<String> preview) {
    }

    private record ReplyCoveragePlan(String mode,
                                     List<String> coveredQuestions,
                                     List<String> deferredQuestions) {
    }
}
