package com.leak.intelligentcustomerchat.domain.knowledge;

import java.util.List;
import java.util.Objects;

public record RetrievalQuery(
        String queryText,
        String scene,
        String subIntent,
        List<String> filters,
        int topK
) {
    public RetrievalQuery {
        Objects.requireNonNull(queryText, "queryText must not be null");
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(subIntent, "subIntent must not be null");
        filters = List.copyOf(filters);
    }
}
