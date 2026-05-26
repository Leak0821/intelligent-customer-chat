package com.leak.intelligentcustomerchat.domain.intent;

import java.util.Objects;

public record IntentRouteResult(
        CustomerScene scene,
        String subIntent,
        ProcessingDisposition disposition,
        String routeReason
) {
    public IntentRouteResult {
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(subIntent, "subIntent must not be null");
        Objects.requireNonNull(disposition, "disposition must not be null");
        Objects.requireNonNull(routeReason, "routeReason must not be null");
    }
}
