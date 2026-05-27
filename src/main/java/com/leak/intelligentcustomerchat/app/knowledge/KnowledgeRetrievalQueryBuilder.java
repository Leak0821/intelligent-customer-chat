package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class KnowledgeRetrievalQueryBuilder {
    public RetrievalQuery build(IntentNormalizationResult normalizationResult,
                                IntentRouteResult routeResult,
                                ContextSnapshot contextSnapshot,
                                BusinessFactResult businessFactResult,
                                RetrievalSettingsConfig retrievalSettings) {
        List<String> segments = new ArrayList<>();
        addSegment(segments, prefixed("primary", normalizationResult.primaryQuestion()));
        addSegment(segments, buildRewriteHint(normalizationResult));
        addSegment(segments, buildSecondaryHint(normalizationResult.secondaryQuestions()));
        addSegment(segments, prefixed("route", normalizeRouteHint(routeResult.subIntent())));
        addSegment(segments, buildSignalHint(contextSnapshot));
        addSegment(segments, buildContextHint(contextSnapshot));
        addSegment(segments, buildFactHint(businessFactResult));

        String queryText = String.join(" | ", segments);
        if (queryText.isBlank()) {
            queryText = normalizeRouteHint(routeResult.subIntent());
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
        return prefixed("context", preview(contextSnapshot.threadSummary(), 160));
    }

    private String buildRewriteHint(IntentNormalizationResult normalizationResult) {
        if (normalizationResult == null) {
            return "";
        }
        String normalizedRequest = preview(normalizationResult.normalizedRequest(), 180);
        String primaryQuestion = preview(normalizationResult.primaryQuestion(), 120);
        if (normalizedRequest.isBlank() || normalizedRequest.equalsIgnoreCase(primaryQuestion)) {
            return "";
        }
        return prefixed("rewrite", normalizedRequest);
    }

    private String buildSecondaryHint(List<String> secondaryQuestions) {
        if (secondaryQuestions == null || secondaryQuestions.isEmpty()) {
            return "";
        }
        List<String> segments = secondaryQuestions.stream()
                .map(value -> preview(value, 100))
                .filter(value -> !value.isBlank())
                .limit(2)
                .toList();
        if (segments.isEmpty()) {
            return "";
        }
        return prefixed("secondary", String.join(" ; ", segments));
    }

    private String buildSignalHint(ContextSnapshot contextSnapshot) {
        if (contextSnapshot == null || contextSnapshot.strongSignals().isEmpty()) {
            return "";
        }
        Set<String> signalLabels = new LinkedHashSet<>();
        for (String signal : contextSnapshot.strongSignals()) {
            if (signal == null || signal.isBlank()) {
                continue;
            }
            if (signal.startsWith("order_id=")) {
                signalLabels.add("order identifier");
                continue;
            }
            if (signal.startsWith("tracking_number=")) {
                signalLabels.add("tracking identifier");
                continue;
            }
            int separatorIndex = signal.indexOf('=');
            signalLabels.add(separatorIndex > 0 ? signal.substring(0, separatorIndex).replace('_', ' ') : signal);
        }
        if (signalLabels.isEmpty()) {
            return "";
        }
        return prefixed("signals", String.join(" ; ", signalLabels));
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
        return prefixed("facts", String.join(" ; ", factSegments));
    }

    private String prefixed(String prefix, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return prefix + " " + value.trim();
    }

    private String preview(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
