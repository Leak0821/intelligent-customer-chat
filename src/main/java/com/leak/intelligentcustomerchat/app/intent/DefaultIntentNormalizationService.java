package com.leak.intelligentcustomerchat.app.intent;

import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class DefaultIntentNormalizationService implements IntentNormalizationService {
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
            "\\b(?:order|tracking)\\s*(?:number|no\\.?|#)?\\s*[:#-]?\\s*([A-Z0-9]{6,20})\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public IntentNormalizationResult normalize(InboundMail mail) {
        String normalizedRequest = (mail.subject() + ". " + mail.rawBody()).replaceAll("\\s+", " ").trim();
        String lowerCaseRequest = normalizedRequest.toLowerCase(Locale.ROOT);

        List<CustomerScene> sceneCandidates = new ArrayList<>();
        if (containsAny(lowerCaseRequest, "order", "tracking", "refund", "return", "shipment", "logistics")) {
            sceneCandidates.add(CustomerScene.AFTER_SALES);
        }
        if (containsAny(lowerCaseRequest, "recommend", "feature", "difference", "stock", "spec", "product")) {
            sceneCandidates.add(CustomerScene.PRE_SALES);
        }
        if (sceneCandidates.isEmpty()) {
            sceneCandidates.add(CustomerScene.UNKNOWN);
        }

        List<String> subIntentCandidates = new ArrayList<>();
        if (containsAny(lowerCaseRequest, "recommend", "best for")) {
            subIntentCandidates.add("product_recommendation");
        }
        if (containsAny(lowerCaseRequest, "difference", "compare")) {
            subIntentCandidates.add("product_comparison");
        }
        if (containsAny(lowerCaseRequest, "stock", "ship")) {
            subIntentCandidates.add("inventory_or_shipping");
        }
        if (containsAny(lowerCaseRequest, "tracking", "where is my order")) {
            subIntentCandidates.add("logistics_tracking");
        }
        if (containsAny(lowerCaseRequest, "refund", "return", "warranty")) {
            subIntentCandidates.add("after_sales_policy");
        }
        if (subIntentCandidates.isEmpty()) {
            subIntentCandidates.add("general_inquiry");
        }

        List<String> requiredEntities = new ArrayList<>();
        List<String> missingEntities = new ArrayList<>();
        ProcessingDisposition disposition = ProcessingDisposition.CONTINUE;

        boolean afterSalesLike = sceneCandidates.contains(CustomerScene.AFTER_SALES);
        boolean hasOrderId = ORDER_ID_PATTERN.matcher(normalizedRequest).find();
        if (afterSalesLike) {
            requiredEntities.add("order_id_or_tracking_no");
            if (!hasOrderId) {
                missingEntities.add("order_id_or_tracking_no");
                disposition = ProcessingDisposition.FOLLOW_UP;
            }
        }
        if (containsAny(lowerCaseRequest, "refund", "compensation", "claim", "angry")) {
            disposition = ProcessingDisposition.HUMAN_REVIEW;
        }

        return new IntentNormalizationResult(
                normalizedRequest,
                normalizedRequest,
                List.of(),
                sceneCandidates,
                subIntentCandidates,
                requiredEntities,
                missingEntities,
                disposition
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
