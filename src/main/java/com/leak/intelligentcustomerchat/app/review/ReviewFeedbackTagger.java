package com.leak.intelligentcustomerchat.app.review;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class ReviewFeedbackTagger {
    public List<String> tagDecision(ReplyDraft draft, ReviewDecisionContext context, String reviewReason) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        String normalizedReason = normalize(reviewReason);
        if (draft.isBlocked() || normalizedReason.contains("manual investigation")) {
            tags.add("system_block");
        }
        if (draft.isFollowUpNeeded() || normalizedReason.contains("missing key entity") || normalizedReason.contains("follow up")) {
            tags.add("clarification_needed");
        }
        if (context.routeResult().scene() == CustomerScene.UNKNOWN || normalizedReason.contains("unknown scene")) {
            tags.add("routing_uncertain");
        }
        if (context.businessFactResult().status() == BusinessFactStatus.CONFLICT
                || context.businessFactResult().status() == BusinessFactStatus.NO_RESULT
                || context.businessFactResult().status() == BusinessFactStatus.TEMPORARY_FAILURE
                || normalizedReason.contains("business facts")) {
            tags.add("fact_gap");
        }
        if (context.routeResult().scene() == CustomerScene.PRE_SALES
                && context.knowledgeRetrieveResult().recallCount() == 0) {
            tags.add("knowledge_gap");
        }
        if (containsAny(normalizedReason, "high risk", "refund", "compensation", "promise")) {
            tags.add("promise_risk");
        }
        if (containsAny(normalizedReason, "policy", "manual approval", "policy safe")) {
            tags.add("policy_boundary");
        }
        if (tags.isEmpty() && draft.isHumanReviewRequired()) {
            tags.add("manual_review_required");
        }
        return List.copyOf(tags);
    }

    public List<String> tagManualReviewNote(String reviewNote) {
        String normalized = normalize(reviewNote);
        if (normalized.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (containsAny(normalized, "refund", "compensation", "promise", "unsupported refund", "unsupported compensation")) {
            tags.add("promise_risk");
        }
        if (containsAny(normalized, "fact", "facts", "verify", "verified", "unsupported", "evidence")) {
            tags.add("fact_gap");
        }
        if (containsAny(normalized, "policy", "policy safe", "manual approval", "eligibility", "warranty")) {
            tags.add("policy_boundary");
        }
        if (containsAny(normalized, "wording", "tone", "phrase", "phrasing", "soften", "stricter wording")) {
            tags.add("tone_risk");
        }
        if (containsAny(normalized, "generic", "clarify", "unclear", "need more information", "follow up")) {
            tags.add("clarification_needed");
        }
        if (!tags.isEmpty()) {
            return List.copyOf(tags);
        }
        if (containsAny(normalized, "approved", "verified", "ready for send", "ready for manual send review")) {
            return List.of();
        }
        return List.of("other");
    }

    private boolean containsAny(String normalized, String... candidates) {
        for (String candidate : candidates) {
            if (normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
