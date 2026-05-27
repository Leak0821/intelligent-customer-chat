package com.leak.intelligentcustomerchat.app.context;

import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContextEntitySignalExtractor {
    private static final Pattern STRUCTURED_ORDER_ID_PATTERN = Pattern.compile("\\border_id=([A-Z0-9]{6,20})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRUCTURED_TRACKING_ID_PATTERN = Pattern.compile("\\btracking_number=([A-Z0-9]{6,24})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
            "\\b(?:order\\s*(?:number|no\\.?|#|id)\\s*(?:is\\s+)?[:#-]?\\s*|my\\s+order\\s+)((?=[A-Z0-9]{6,20}\\b)(?=[A-Z0-9]*\\d)[A-Z0-9]{6,20})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TRACKING_ID_PATTERN = Pattern.compile(
            "\\b(?:tracking\\s*(?:number|no\\.?|#|id)\\s*(?:is\\s+)?[:#-]?\\s*|my\\s+tracking\\s+)((?=[A-Z0-9]{6,24}\\b)(?=[A-Z0-9]*\\d)[A-Z0-9]{6,24})\\b",
            Pattern.CASE_INSENSITIVE
    );

    public ContextEntitySignals extract(ContextSnapshot contextSnapshot) {
        return extract(
                contextSnapshot.threadSummary(),
                contextSnapshot.strongSignals(),
                contextSnapshot.optionalSignals()
        );
    }

    public ContextEntitySignals extract(String summary,
                                        List<String> primarySignals,
                                        List<String> secondarySignals) {
        List<String> texts = new ArrayList<>();
        addText(texts, summary);
        texts.addAll(primarySignals);
        texts.addAll(secondarySignals);

        String orderId = null;
        String trackingNumber = null;
        Set<String> strongSignals = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            if (orderId == null) {
                orderId = extractFirstMatch(STRUCTURED_ORDER_ID_PATTERN, text);
                if (orderId == null) {
                    orderId = extractFirstMatch(ORDER_ID_PATTERN, text);
                }
                if (orderId != null) {
                    strongSignals.add("order_id=" + orderId);
                }
            }
            if (trackingNumber == null) {
                trackingNumber = extractFirstMatch(STRUCTURED_TRACKING_ID_PATTERN, text);
                if (trackingNumber == null) {
                    trackingNumber = extractFirstMatch(TRACKING_ID_PATTERN, text);
                }
                if (trackingNumber != null) {
                    strongSignals.add("tracking_number=" + trackingNumber);
                }
            }
            if (orderId != null && trackingNumber != null) {
                break;
            }
        }
        return new ContextEntitySignals(orderId, trackingNumber, List.copyOf(strongSignals));
    }

    private void addText(List<String> texts, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        texts.add(value);
    }

    private String extractFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
