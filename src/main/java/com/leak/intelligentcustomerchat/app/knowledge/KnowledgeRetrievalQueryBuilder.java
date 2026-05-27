package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class KnowledgeRetrievalQueryBuilder {
    public RetrievalQuery build(IntentNormalizationResult normalizationResult,
                                IntentRouteResult routeResult,
                                ContextSnapshot contextSnapshot,
                                BusinessFactResult businessFactResult,
                                RetrievalSettingsConfig retrievalSettings) {
        List<String> segments = new ArrayList<>();
        addSegment(segments, normalizationResult.primaryQuestion());
        addSegment(segments, normalizeRouteHint(routeResult.subIntent()));
        addSegment(segments, buildContextHint(contextSnapshot));
        addSegment(segments, buildFactHint(businessFactResult));

        String queryText = String.join(" | ", segments);
        if (queryText.isBlank()) {
            queryText = routeResult.subIntent();
        }
        return new RetrievalQuery(
                queryText,
                routeResult.scene().name(),
                routeResult.subIntent(),
                businessFactResult.resolvedEntities(),
                retrievalSettings.topK()
        );
    }

    private void addSegment(List<String> segments, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        segments.add(value.trim());
    }

    private String normalizeRouteHint(String subIntent) {
        if (subIntent == null || subIntent.isBlank()) {
            return "";
        }
        return switch (subIntent) {
            case "product_recommendation" -> "product recommendation suitable use case feature";
            case "product_comparison" -> "product comparison difference feature use case";
            case "inventory_or_shipping" -> "stock availability shipping delivery time";
            case "order_status" -> "order status shipping stage processing";
            case "logistics_tracking" -> "logistics tracking delivery update shipping timeline";
            case "after_sales_policy" -> "return refund warranty replacement policy process";
            case "return_refund" -> "refund compensation return policy exception manual review";
            default -> subIntent.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    private String buildContextHint(ContextSnapshot contextSnapshot) {
        if (contextSnapshot == null || contextSnapshot.threadSummary() == null || contextSnapshot.threadSummary().isBlank()) {
            return "";
        }
        return "context " + preview(contextSnapshot.threadSummary(), 160);
    }

    private String buildFactHint(BusinessFactResult businessFactResult) {
        if (businessFactResult == null) {
            return "";
        }
        List<String> factSegments = new ArrayList<>();
        for (String fact : businessFactResult.facts().stream().limit(2).toList()) {
            factSegments.add(preview(fact, 80));
        }
        for (String flag : businessFactResult.conflictFlags().stream().limit(1).toList()) {
            factSegments.add(preview(flag, 60));
        }
        if (factSegments.isEmpty()) {
            return "";
        }
        return "facts " + String.join(" ; ", factSegments);
    }

    private String preview(String value, int maxLength) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
