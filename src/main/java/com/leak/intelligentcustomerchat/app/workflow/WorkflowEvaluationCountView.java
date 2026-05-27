package com.leak.intelligentcustomerchat.app.workflow;

import java.util.Objects;

public record WorkflowEvaluationCountView(
        String key,
        long count
) {
    public WorkflowEvaluationCountView {
        Objects.requireNonNull(key, "key must not be null");
    }
}
