package com.leak.intelligentcustomerchat.app.workflow;

import java.util.LinkedHashSet;
import java.util.List;

final class WorkflowRiskDecisionResolver {
    private WorkflowRiskDecisionResolver() {
    }

    static WorkflowRiskDecisionView resolve(String workflowStatus,
                                            String draftStatus,
                                            String sendReadiness,
                                            String nextAction,
                                            String latestDispatchStatus,
                                            String latestReviewAction,
                                            String manualReviewOutcome,
                                            List<String> riskFlags) {
        String normalizedWorkflowStatus = normalize(workflowStatus, "UNKNOWN");
        String normalizedDraftStatus = normalize(draftStatus, "UNKNOWN");
        String normalizedSendReadiness = normalize(sendReadiness, "UNKNOWN");
        String normalizedDispatchStatus = normalize(latestDispatchStatus, "NONE");
        String normalizedReviewAction = normalize(latestReviewAction, "NONE");
        String normalizedReviewOutcome = normalize(manualReviewOutcome, "NOT_REVIEWED");
        List<String> safeRiskFlags = riskFlags == null ? List.of() : List.copyOf(riskFlags);

        String riskLevel = resolveRiskLevel(normalizedWorkflowStatus, normalizedDispatchStatus, safeRiskFlags);
        String releaseDecision = resolveReleaseDecision(
                normalizedWorkflowStatus,
                normalizedDraftStatus,
                normalizedSendReadiness,
                normalizedDispatchStatus
        );
        boolean sendAllowed = "READY_FOR_DISPATCH".equals(releaseDecision) || "DISPATCHED".equals(releaseDecision);
        List<String> blockingReasons = buildBlockingReasons(releaseDecision, safeRiskFlags);
        List<String> decisionSignals = buildDecisionSignals(
                normalizedWorkflowStatus,
                normalizedDraftStatus,
                normalizedSendReadiness,
                normalizedDispatchStatus,
                normalizedReviewAction,
                normalizedReviewOutcome,
                safeRiskFlags
        );

        return new WorkflowRiskDecisionView(
                riskLevel,
                releaseDecision,
                sendAllowed,
                resolveRecommendedAction(nextAction, releaseDecision),
                blockingReasons,
                decisionSignals
        );
    }

    private static String resolveRiskLevel(String workflowStatus,
                                           String latestDispatchStatus,
                                           List<String> riskFlags) {
        if ("BLOCKED".equals(workflowStatus)
                || "FAILED_FINAL".equals(latestDispatchStatus)
                || riskFlags.contains("dispatch_failed_final")) {
            return "CRITICAL";
        }
        if (riskFlags.contains("manual_review_required")
                || riskFlags.contains("review_rejected")
                || riskFlags.contains("business_fact_conflict")) {
            return "HIGH";
        }
        if (riskFlags.contains("follow_up_needed")
                || riskFlags.contains("dispatch_retry_pending")
                || riskFlags.contains("business_fact_insufficient_input")) {
            return "MEDIUM";
        }
        if (!riskFlags.isEmpty()) {
            return "LOW";
        }
        return "MINIMAL";
    }

    private static String resolveReleaseDecision(String workflowStatus,
                                                 String draftStatus,
                                                 String sendReadiness,
                                                 String latestDispatchStatus) {
        if ("BLOCKED".equals(workflowStatus)) {
            return "BLOCKED";
        }
        if ("SENT".equals(latestDispatchStatus)) {
            return "DISPATCHED";
        }
        if ("FAILED_FINAL".equals(latestDispatchStatus)) {
            return "MANUAL_INTERVENTION_REQUIRED";
        }
        if ("RETRY_PENDING".equals(latestDispatchStatus)) {
            return "RETRY_PENDING";
        }
        if ("READY_FOR_SEND".equals(sendReadiness)) {
            return "READY_FOR_DISPATCH";
        }
        if ("PENDING_REVIEW".equals(sendReadiness) || "HUMAN_REVIEW_REQUIRED".equals(draftStatus)) {
            return "HOLD_FOR_REVIEW";
        }
        if ("FOLLOW_UP_NEEDED".equals(draftStatus) || "NOT_APPLICABLE".equals(sendReadiness)) {
            return "NEED_CUSTOMER_FOLLOW_UP";
        }
        if ("DRAFT_READY".equals(draftStatus)) {
            return "DRAFT_READY";
        }
        return "OBSERVE";
    }

    private static String resolveRecommendedAction(String nextAction, String releaseDecision) {
        if (nextAction != null && !nextAction.isBlank()) {
            return nextAction;
        }
        return switch (releaseDecision) {
            case "BLOCKED", "MANUAL_INTERVENTION_REQUIRED" -> "manual_investigation";
            case "RETRY_PENDING" -> "await_retry_dispatch";
            case "HOLD_FOR_REVIEW" -> "await_review_decision";
            case "NEED_CUSTOMER_FOLLOW_UP" -> "send_follow_up_question";
            case "READY_FOR_DISPATCH" -> "dispatch_reply";
            case "DISPATCHED" -> "observe_delivery_result";
            case "DRAFT_READY" -> "approve_then_dispatch";
            default -> "inspect_workflow";
        };
    }

    private static List<String> buildBlockingReasons(String releaseDecision, List<String> riskFlags) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        switch (releaseDecision) {
            case "BLOCKED" -> reasons.add("workflow_blocked");
            case "MANUAL_INTERVENTION_REQUIRED" -> reasons.add("dispatch_failed_final");
            case "RETRY_PENDING" -> reasons.add("dispatch_retry_pending");
            case "HOLD_FOR_REVIEW" -> reasons.add("manual_review_required");
            case "NEED_CUSTOMER_FOLLOW_UP" -> reasons.add("follow_up_needed");
            default -> {
            }
        }
        riskFlags.stream()
                .filter(flag -> flag.contains("conflict")
                        || flag.contains("insufficient_input")
                        || flag.contains("review")
                        || flag.contains("blocked")
                        || flag.contains("retry")
                        || flag.contains("failed"))
                .forEach(reasons::add);
        return List.copyOf(reasons);
    }

    private static List<String> buildDecisionSignals(String workflowStatus,
                                                     String draftStatus,
                                                     String sendReadiness,
                                                     String latestDispatchStatus,
                                                     String latestReviewAction,
                                                     String manualReviewOutcome,
                                                     List<String> riskFlags) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        signals.add("workflow_status=" + workflowStatus);
        signals.add("draft_status=" + draftStatus);
        signals.add("send_readiness=" + sendReadiness);
        if (!"NONE".equals(latestDispatchStatus)) {
            signals.add("dispatch_status=" + latestDispatchStatus);
        }
        if (!"NONE".equals(latestReviewAction)) {
            signals.add("latest_review_action=" + latestReviewAction);
        }
        if (!"NOT_REVIEWED".equals(manualReviewOutcome)) {
            signals.add("manual_review_outcome=" + manualReviewOutcome);
        }
        riskFlags.stream().limit(4).map(flag -> "risk_flag=" + flag).forEach(signals::add);
        return List.copyOf(signals);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
