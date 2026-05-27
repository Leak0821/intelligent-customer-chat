package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class IntentNormalizationHeuristics {
    static final String ORDER_ID_ENTITY = "order_id_or_tracking_no";
    static final String PRE_PURCHASE_POLICY_SIGNAL = "pre_purchase_policy_question";
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
            "\\b(?:order|tracking)\\s*(?:number|no\\.?|#|id)\\s*(?:is\\s+)?[:#-]?\\s*([A-Z0-9-]{6,32})\\b",
            Pattern.CASE_INSENSITIVE
    );

    IntentNormalizationResult normalize(InboundMail mail) {
        return analyze(mail).result();
    }

    HeuristicAnalysis analyze(InboundMail mail) {
        String mergedMailText = mergeMailText(mail.subject(), mail.rawBody());
        QuestionRewrite questionRewrite = rewriteQuestions(mail.subject(), mail.rawBody(), mergedMailText);
        String lowerCaseRequest = mergedMailText.toLowerCase(Locale.ROOT);
        List<String> matchedSignals = new ArrayList<>();

        boolean hasExplicitOrderId = ORDER_ID_PATTERN.matcher(mergedMailText).find();
        boolean prePurchasePolicyQuestion = isPrePurchasePolicyQuestion(lowerCaseRequest);
        boolean preSalesPlanningQuestion = isPreSalesPlanningQuestion(lowerCaseRequest);
        boolean clearAfterSalesIssue = isClearAfterSalesIssue(lowerCaseRequest);
        boolean ownedOrderReference = hasOwnedOrderReference(lowerCaseRequest);
        boolean afterSalesPolicyQuestion = isAfterSalesPolicyQuestion(lowerCaseRequest);
        boolean highRiskAfterSalesRequest = isHighRiskAfterSalesRequest(lowerCaseRequest);

        List<CustomerScene> sceneCandidates = new ArrayList<>();
        if (prePurchasePolicyQuestion) {
            matchedSignals.add(PRE_PURCHASE_POLICY_SIGNAL);
        }
        if (hasExplicitOrderId) {
            matchedSignals.add("explicit_order_or_tracking_id");
        }

        boolean afterSalesLike = hasExplicitOrderId
                || clearAfterSalesIssue
                || ownedOrderReference
                || (afterSalesPolicyQuestion && !prePurchasePolicyQuestion);
        boolean preSalesLike = preSalesPlanningQuestion || prePurchasePolicyQuestion;

        if (prePurchasePolicyQuestion && !hasExplicitOrderId && !clearAfterSalesIssue) {
            sceneCandidates.add(CustomerScene.PRE_SALES);
            matchedSignals.add("scene_pre_sales_policy_before_purchase");
        } else {
            if (afterSalesLike) {
                sceneCandidates.add(CustomerScene.AFTER_SALES);
                matchedSignals.add("scene_after_sales");
            }
            if (preSalesLike) {
                sceneCandidates.add(CustomerScene.PRE_SALES);
                matchedSignals.add("scene_pre_sales");
            }
        }
        if (sceneCandidates.isEmpty()) {
            sceneCandidates.add(CustomerScene.UNKNOWN);
            matchedSignals.add("scene_unknown_fallback");
        }

        List<String> subIntentCandidates = new ArrayList<>();
        if (containsAny(lowerCaseRequest, "recommend", "best for", "suitable for", "which one should i buy")) {
            subIntentCandidates.add("product_recommendation");
            matchedSignals.add("sub_intent_product_recommendation");
        }
        if (containsAny(lowerCaseRequest, "difference", "compare", "versus", "vs")) {
            subIntentCandidates.add("product_comparison");
            matchedSignals.add("sub_intent_product_comparison");
        }
        if (containsAny(lowerCaseRequest,
                "stock",
                "in stock",
                "ship to",
                "shipping to",
                "delivery time",
                "shipping time",
                "how long does shipping take",
                "how long will shipping take")) {
            subIntentCandidates.add("inventory_or_shipping");
            matchedSignals.add("sub_intent_inventory_or_shipping");
        }
        if (containsAny(lowerCaseRequest,
                "tracking",
                "where is my order",
                "shipment status",
                "logistics update",
                "logistics status",
                "parcel status",
                "delivery update")) {
            subIntentCandidates.add("logistics_tracking");
            matchedSignals.add("sub_intent_logistics_tracking");
        }
        if (containsAny(lowerCaseRequest,
                "order status",
                "current order status",
                "when will my order ship",
                "when will it ship",
                "processing order",
                "has my order shipped",
                "is my order confirmed")) {
            subIntentCandidates.add("order_status");
            matchedSignals.add("sub_intent_order_status");
        }
        if (!prePurchasePolicyQuestion && isReturnRefundRequest(lowerCaseRequest)) {
            subIntentCandidates.add("return_refund");
            matchedSignals.add("sub_intent_return_refund");
        }
        if (!prePurchasePolicyQuestion && containsAny(lowerCaseRequest, "refund", "return", "warranty", "replacement", "replace")) {
            subIntentCandidates.add("after_sales_policy");
            matchedSignals.add("sub_intent_after_sales_policy");
        }
        if (subIntentCandidates.isEmpty()) {
            subIntentCandidates.add("general_inquiry");
            matchedSignals.add("sub_intent_general_inquiry_fallback");
        }

        List<String> requiredEntities = new ArrayList<>();
        List<String> missingEntities = new ArrayList<>();
        ProcessingDisposition disposition = ProcessingDisposition.CONTINUE;

        if (afterSalesLike) {
            requiredEntities.add(ORDER_ID_ENTITY);
            matchedSignals.add("require_order_id_for_after_sales");
            if (!hasExplicitOrderId) {
                missingEntities.add(ORDER_ID_ENTITY);
                disposition = ProcessingDisposition.FOLLOW_UP;
                matchedSignals.add("disposition_follow_up_missing_order_id");
            }
        }
        if (highRiskAfterSalesRequest) {
            disposition = ProcessingDisposition.HUMAN_REVIEW;
            matchedSignals.add("disposition_human_review_high_risk_after_sales");
        }

        return new HeuristicAnalysis(
                new IntentNormalizationResult(
                        questionRewrite.normalizedRequest(),
                        questionRewrite.primaryQuestion(),
                        questionRewrite.secondaryQuestions(),
                        sceneCandidates,
                        subIntentCandidates,
                        requiredEntities,
                        missingEntities,
                        disposition
                ),
                List.copyOf(matchedSignals)
        );
    }

    boolean hasExplicitOrderOrTrackingId(String text) {
        return ORDER_ID_PATTERN.matcher(text).find();
    }

    String mergeMailText(String subject, String body) {
        return (subject + ". " + body).replaceAll("\\s+", " ").trim();
    }

    private QuestionRewrite rewriteQuestions(String subject, String body, String mergedMailText) {
        List<String> actionableSegments = extractActionableSegments(body);
        if (actionableSegments.isEmpty()) {
            String fallback = normalizeSegment(mergedMailText);
            return new QuestionRewrite(fallback, fallback, List.of());
        }

        String primaryQuestion = actionableSegments.get(0);
        List<String> secondaryQuestions = actionableSegments.size() > 1
                ? List.copyOf(actionableSegments.subList(1, Math.min(actionableSegments.size(), 4)))
                : List.of();

        if (looksLikePureGreeting(primaryQuestion)) {
            primaryQuestion = normalizeSegment(mergeMailText(subject, body));
            secondaryQuestions = List.of();
        }

        String normalizedRequest = secondaryQuestions.isEmpty()
                ? primaryQuestion
                : primaryQuestion + " Secondary questions: " + String.join("; ", secondaryQuestions);
        return new QuestionRewrite(normalizedRequest, primaryQuestion, secondaryQuestions);
    }

    private List<String> extractActionableSegments(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        String[] rawSegments = body.replace('\r', '\n').split("[\\n!?;]+");
        Set<String> actionable = new LinkedHashSet<>();
        for (String rawSegment : rawSegments) {
            String segment = normalizeSegment(rawSegment);
            if (segment.isBlank()) {
                continue;
            }
            if (isGreetingOrSignoff(segment)) {
                continue;
            }
            actionable.add(segment);
        }
        return List.copyOf(actionable);
    }

    private boolean isGreetingOrSignoff(String segment) {
        String lowerCase = segment.toLowerCase(Locale.ROOT);
        return looksLikePureGreeting(segment)
                || lowerCase.startsWith("thanks")
                || lowerCase.startsWith("thank you")
                || lowerCase.startsWith("best regards")
                || lowerCase.startsWith("kind regards")
                || lowerCase.startsWith("regards")
                || lowerCase.startsWith("sent from");
    }

    private boolean looksLikePureGreeting(String segment) {
        String lowerCase = segment.toLowerCase(Locale.ROOT);
        return lowerCase.equals("hello")
                || lowerCase.equals("hi")
                || lowerCase.equals("dear team")
                || lowerCase.equals("dear support")
                || lowerCase.equals("hello team")
                || lowerCase.equals("hello support")
                || lowerCase.equals("hi team")
                || lowerCase.equals("hi support");
    }

    private String normalizeSegment(String value) {
        return value.replaceAll("\\s+", " ").trim()
                .replaceAll("^[,.:\\-\\s]+", "")
                .replaceAll("[,.:\\-\\s]+$", "");
    }

    private boolean isPreSalesPlanningQuestion(String text) {
        return containsAny(text,
                "recommend",
                "best for",
                "difference",
                "compare",
                "versus",
                "vs",
                "stock",
                "spec",
                "feature",
                "product",
                "catalog",
                "app control",
                "color temperature",
                "ship to",
                "shipping to",
                "before i order",
                "before ordering",
                "before i buy",
                "before buying",
                "plan to buy",
                "planning to buy",
                "want to buy",
                "thinking of buying",
                "looking for",
                "suitable for");
    }

    private boolean isPrePurchasePolicyQuestion(String text) {
        return containsAny(text, "return policy", "refund policy", "warranty policy")
                && containsAny(text,
                "before i order",
                "before ordering",
                "before i buy",
                "before buying",
                "plan to buy",
                "planning to buy",
                "want to buy",
                "thinking of buying");
    }

    private boolean isClearAfterSalesIssue(String text) {
        return containsAny(text,
                "tracking",
                "where is my order",
                "order status",
                "shipment status",
                "logistics",
                "delivery update",
                "parcel status",
                "received",
                "arrived",
                "damaged",
                "broken",
                "wrong item",
                "cancel my order");
    }

    private boolean isAfterSalesPolicyQuestion(String text) {
        return containsAny(text, "refund", "return", "warranty", "replacement", "replace");
    }

    private boolean isReturnRefundRequest(String text) {
        return containsAny(text,
                "want a refund",
                "need a refund",
                "request a refund",
                "refund and compensation",
                "want compensation",
                "need compensation",
                "file a claim",
                "open a claim",
                "cancel my order",
                "return this order",
                "return this product",
                "replace this item");
    }

    private boolean isHighRiskAfterSalesRequest(String text) {
        return containsAny(text,
                "refund and compensation",
                "want compensation",
                "need compensation",
                "file a claim",
                "open a claim",
                "chargeback",
                "legal action",
                "angry");
    }

    private boolean hasOwnedOrderReference(String text) {
        return containsAny(text,
                "my order",
                "the order i placed",
                "my shipment",
                "my package");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    record HeuristicAnalysis(
            IntentNormalizationResult result,
            List<String> matchedSignals
    ) {
    }

    private record QuestionRewrite(
            String normalizedRequest,
            String primaryQuestion,
            List<String> secondaryQuestions
    ) {
    }
}
