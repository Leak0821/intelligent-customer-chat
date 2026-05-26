package com.leak.intelligentcustomerchat.app.business;

import com.leak.intelligentcustomerchat.domain.business.AfterSalesPolicyResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.business.BusinessFactStatus;
import com.leak.intelligentcustomerchat.domain.business.BusinessQueryContext;
import com.leak.intelligentcustomerchat.domain.context.ContextSnapshot;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.business.GatewayQueryStatus;
import com.leak.intelligentcustomerchat.domain.business.LogisticsQueryResult;
import com.leak.intelligentcustomerchat.domain.business.OrderQueryResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import com.leak.intelligentcustomerchat.infrastructure.business.AfterSalesPolicyGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.LogisticsQueryGateway;
import com.leak.intelligentcustomerchat.infrastructure.business.OrderQueryGateway;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DefaultBusinessFactService implements BusinessFactService {
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
            "\\border\\s*(?:number|no\\.?|#)?\\s*[:#-]?\\s*([A-Z0-9]{6,20})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TRACKING_ID_PATTERN = Pattern.compile(
            "\\btracking\\s*(?:number|no\\.?|#)?\\s*[:#-]?\\s*([A-Z0-9]{6,24})\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final OrderQueryGateway orderQueryGateway;
    private final LogisticsQueryGateway logisticsQueryGateway;
    private final AfterSalesPolicyGateway afterSalesPolicyGateway;

    public DefaultBusinessFactService(OrderQueryGateway orderQueryGateway,
                                      LogisticsQueryGateway logisticsQueryGateway,
                                      AfterSalesPolicyGateway afterSalesPolicyGateway) {
        this.orderQueryGateway = orderQueryGateway;
        this.logisticsQueryGateway = logisticsQueryGateway;
        this.afterSalesPolicyGateway = afterSalesPolicyGateway;
    }

    @Override
    public BusinessFactResult loadFacts(InboundMail mail,
                                        IntentNormalizationResult normalizationResult,
                                        IntentRouteResult routeResult,
                                        ContextSnapshot contextSnapshot) {
        if (routeResult.scene() == CustomerScene.PRE_SALES || routeResult.scene() == CustomerScene.UNKNOWN) {
            return BusinessFactResult.notRequired();
        }
        if (!normalizationResult.missingEntities().isEmpty()) {
            return BusinessFactResult.insufficientInput(normalizationResult.missingEntities());
        }

        BusinessQueryContext queryContext = new BusinessQueryContext(
                mail.from(),
                mail.threadId(),
                routeResult.scene().name(),
                routeResult.subIntent(),
                extractFirstMatch(ORDER_ID_PATTERN, normalizationResult.normalizedRequest()),
                extractFirstMatch(TRACKING_ID_PATTERN, normalizationResult.normalizedRequest()),
                normalizationResult.primaryQuestion()
        );

        List<GatewayFactSlice> results = new ArrayList<>();
        if (requiresOrderFacts(routeResult.subIntent())) {
            results.add(toGatewayFactSlice(orderQueryGateway.query(queryContext)));
        }
        if (requiresLogisticsFacts(routeResult.subIntent())) {
            results.add(toGatewayFactSlice(logisticsQueryGateway.query(queryContext)));
        }
        if (requiresPolicyFacts(routeResult.subIntent())) {
            results.add(toGatewayFactSlice(afterSalesPolicyGateway.query(queryContext)));
        }
        if (results.isEmpty()) {
            results.add(toGatewayFactSlice(afterSalesPolicyGateway.query(queryContext)));
        }

        return merge(results);
    }

    private boolean requiresOrderFacts(String subIntent) {
        return "order_status".equals(subIntent)
                || "logistics_tracking".equals(subIntent)
                || "after_sales_policy".equals(subIntent);
    }

    private boolean requiresLogisticsFacts(String subIntent) {
        return "logistics_tracking".equals(subIntent);
    }

    private boolean requiresPolicyFacts(String subIntent) {
        return "after_sales_policy".equals(subIntent);
    }

    private BusinessFactResult merge(List<GatewayFactSlice> results) {
        List<String> sourceSystems = new ArrayList<>();
        List<String> resolvedEntities = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        List<String> missingEntities = new ArrayList<>();
        List<String> conflictFlags = new ArrayList<>();

        BusinessFactStatus mergedStatus = BusinessFactStatus.SUCCESS;
        OffsetDateTime latestTimestamp = results.get(0).queriedAt();
        for (GatewayFactSlice result : results) {
            sourceSystems.add(result.sourceSystem());
            resolvedEntities.addAll(result.resolvedEntities());
            facts.addAll(result.facts());
            missingEntities.addAll(result.missingEntities());
            conflictFlags.addAll(result.conflictFlags());
            mergedStatus = mergeStatus(mergedStatus, result.status());
            if (result.queriedAt().isAfter(latestTimestamp)) {
                latestTimestamp = result.queriedAt();
            }
        }

        return new BusinessFactResult(
                mergedStatus,
                String.join(",", sourceSystems),
                resolvedEntities,
                facts,
                missingEntities,
                conflictFlags,
                latestTimestamp
        );
    }

    private GatewayFactSlice toGatewayFactSlice(OrderQueryResult result) {
        List<String> resolvedEntities = result.orderId() == null || result.orderId().isBlank()
                ? List.of()
                : List.of("order_id=" + result.orderId());
        List<String> facts = result.status() == GatewayQueryStatus.SUCCESS
                ? List.of(
                "order status=" + result.orderStatus(),
                "payment status=" + result.paymentStatus(),
                "order owner email=" + result.customerEmail()
        )
                : List.of();
        return new GatewayFactSlice(
                mapStatus(result.status()),
                result.sourceSystem(),
                resolvedEntities,
                facts,
                result.missingEntities(),
                result.conflictFlags(),
                result.queriedAt()
        );
    }

    private GatewayFactSlice toGatewayFactSlice(LogisticsQueryResult result) {
        List<String> resolvedEntities = new ArrayList<>();
        if (result.orderId() != null && !result.orderId().isBlank()) {
            resolvedEntities.add("order_id=" + result.orderId());
        }
        if (result.trackingNumber() != null && !result.trackingNumber().isBlank()) {
            resolvedEntities.add("tracking_number=" + result.trackingNumber());
        }
        List<String> facts = result.status() == GatewayQueryStatus.SUCCESS
                ? List.of(
                "latest logistics node=" + result.latestNode(),
                result.etaHint(),
                "current logistics status=" + result.logisticsStatus()
        )
                : List.of();
        return new GatewayFactSlice(
                mapStatus(result.status()),
                result.sourceSystem(),
                resolvedEntities,
                facts,
                result.missingEntities(),
                result.conflictFlags(),
                result.queriedAt()
        );
    }

    private GatewayFactSlice toGatewayFactSlice(AfterSalesPolicyResult result) {
        List<String> facts = result.status() == GatewayQueryStatus.SUCCESS
                ? result.policyNotes()
                : List.of();
        return new GatewayFactSlice(
                mapStatus(result.status()),
                result.sourceSystem(),
                result.policyCode() == null || result.policyCode().isBlank()
                        ? List.of()
                        : List.of("policy_code=" + result.policyCode()),
                facts,
                result.missingEntities(),
                result.conflictFlags(),
                result.queriedAt()
        );
    }

    private BusinessFactStatus mergeStatus(BusinessFactStatus current, BusinessFactStatus incoming) {
        if (incoming == BusinessFactStatus.CONFLICT) {
            return BusinessFactStatus.CONFLICT;
        }
        if (incoming == BusinessFactStatus.TEMPORARY_FAILURE && current != BusinessFactStatus.CONFLICT) {
            return BusinessFactStatus.TEMPORARY_FAILURE;
        }
        if (incoming == BusinessFactStatus.INSUFFICIENT_INPUT && current == BusinessFactStatus.SUCCESS) {
            return BusinessFactStatus.INSUFFICIENT_INPUT;
        }
        if (incoming == BusinessFactStatus.NO_RESULT && current == BusinessFactStatus.SUCCESS) {
            return BusinessFactStatus.NO_RESULT;
        }
        return current;
    }

    private BusinessFactStatus mapStatus(GatewayQueryStatus status) {
        return switch (status) {
            case SUCCESS -> BusinessFactStatus.SUCCESS;
            case INSUFFICIENT_INPUT -> BusinessFactStatus.INSUFFICIENT_INPUT;
            case NO_RESULT -> BusinessFactStatus.NO_RESULT;
            case TEMPORARY_FAILURE -> BusinessFactStatus.TEMPORARY_FAILURE;
            case CONFLICT -> BusinessFactStatus.CONFLICT;
        };
    }

    private String extractFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private record GatewayFactSlice(
            BusinessFactStatus status,
            String sourceSystem,
            List<String> resolvedEntities,
            List<String> facts,
            List<String> missingEntities,
            List<String> conflictFlags,
            OffsetDateTime queriedAt
    ) {
    }
}
