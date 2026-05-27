package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

@Component
public class WorkflowEvidenceSummaryParser {

    public String summarizeBusinessFacts(String subIntent, String businessFactsSummary) {
        Map<String, String> tokens = parseTokens(businessFactsSummary);
        String factStatus = tokenOrDefault(tokens, "factStatus", "UNKNOWN");
        return inferFactRole(factStatus, normalizeSubIntent(subIntent));
    }

    public String summarizeKnowledgeRole(String subIntent) {
        return inferKnowledgeRole(normalizeSubIntent(subIntent));
    }

    public Map<String, String> parseTokens(String summary) {
        Map<String, String> tokens = new LinkedHashMap<>();
        if (summary == null || summary.isBlank()) {
            return tokens;
        }
        for (String part : summary.split(",")) {
            int separator = part.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = part.substring(0, separator).trim();
            String value = part.substring(separator + 1).trim();
            if (!key.isBlank()) {
                tokens.put(key, value);
            }
        }
        return tokens;
    }

    public List<String> splitPipeList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : rawValue.split("\\|")) {
            String normalized = part.trim();
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    public int parseInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public String tokenOrDefault(Map<String, String> tokens, String key, String fallback) {
        String value = tokens.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String buildBusinessFactsStageSummary(com.leak.intelligentcustomerchat.domain.business.BusinessFactResult businessFactResult) {
        return "factStatus=%s, sourceSystems=%s, resolvedEntityCount=%s, factCount=%s, missingEntityCount=%s, conflictFlagCount=%s"
                .formatted(
                        businessFactResult.status(),
                        sanitizeForSummary(String.join("|", splitSourceSystems(businessFactResult.sourceSystem()))),
                        businessFactResult.resolvedEntities().size(),
                        businessFactResult.facts().size(),
                        businessFactResult.missingEntities().size(),
                        businessFactResult.conflictFlags().size()
                );
    }

    public String buildKnowledgeStageSummary(KnowledgeRetrieveResult knowledgeRetrieveResult) {
        return "knowledgeRecallCount=%s, retrievalSource=%s, snippetIds=%s"
                .formatted(
                        knowledgeRetrieveResult.recallCount(),
                        sanitizeForSummary(knowledgeRetrieveResult.source()),
                        sanitizeForSummary(String.join("|", knowledgeRetrieveResult.snippets().stream().map(com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet::id).toList()))
                );
    }

    private List<String> splitSourceSystems(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(sourceSystem.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String inferFactRole(String factStatus, String subIntent) {
        String normalizedStatus = factStatus.toUpperCase(Locale.ROOT);
        if ("NOT_REQUIRED".equals(normalizedStatus)) {
            return "business facts are not required for this route";
        }
        if ("INSUFFICIENT_INPUT".equals(normalizedStatus)) {
            return "business facts are blocked until key identifiers are provided";
        }
        if ("CONFLICT".equals(normalizedStatus)) {
            return "business facts act as an authority check and currently conflict";
        }
        if ("NO_RESULT".equals(normalizedStatus)) {
            return "business facts were queried but did not return a usable record";
        }
        if ("TEMPORARY_FAILURE".equals(normalizedStatus)) {
            return "business facts are required but the upstream query is temporarily unavailable";
        }
        return switch (subIntent) {
            case "logistics_tracking" -> "business facts provide the latest order and logistics truth";
            case "order_status" -> "business facts provide the current order truth";
            case "after_sales_policy" -> "business facts provide order truth before policy guidance is applied";
            default -> "business facts provide authoritative context for this route";
        };
    }

    private String inferKnowledgeRole(String subIntent) {
        return switch (subIntent) {
            case "product_recommendation", "product_comparison", "inventory_or_shipping" ->
                    "knowledge fills product and catalog guidance that business facts do not provide";
            case "after_sales_policy" ->
                    "knowledge supplements policy wording and handling guidance after business facts are checked";
            case "logistics_tracking", "order_status" ->
                    "knowledge supplements explanation and expectation setting around the current business facts";
            default -> "knowledge supplements general response guidance for the current route";
        };
    }

    private String sanitizeForSummary(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        return value.replace(",", "/").replaceAll("\\s+", " ").trim();
    }

    private String normalizeSubIntent(String subIntent) {
        if (subIntent == null || subIntent.isBlank()) {
            return "";
        }
        return subIntent.trim().toLowerCase(Locale.ROOT);
    }
}
