package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.domain.reply.ReplyDraft;
import com.leak.intelligentcustomerchat.domain.review.ReviewDecision;

import java.util.Objects;

public record WorkflowAnalysisView(
        InboundMail cleanedMail,
        IntentNormalizationResult normalizationResult,
        IntentRouteResult routeResult,
        ContextSnapshot contextSnapshot,
        BusinessFactResult businessFactResult,
        KnowledgeRetrieveResult knowledgeRetrieveResult,
        ReplyDraft draft,
        ReviewDecision reviewDecision
) {
    public WorkflowAnalysisView {
        Objects.requireNonNull(cleanedMail, "cleanedMail must not be null");
        Objects.requireNonNull(normalizationResult, "normalizationResult must not be null");
        Objects.requireNonNull(routeResult, "routeResult must not be null");
        Objects.requireNonNull(contextSnapshot, "contextSnapshot must not be null");
        Objects.requireNonNull(businessFactResult, "businessFactResult must not be null");
        Objects.requireNonNull(knowledgeRetrieveResult, "knowledgeRetrieveResult must not be null");
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(reviewDecision, "reviewDecision must not be null");
    }
}
