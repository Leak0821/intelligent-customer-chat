package com.leak.intelligentcustomerchat.domain.runtime;

import java.util.List;

public record IntentCatalogConfig(
        List<String> preSalesIntents,
        List<String> afterSalesIntents
) {
    public IntentCatalogConfig {
        preSalesIntents = List.copyOf(preSalesIntents);
        afterSalesIntents = List.copyOf(afterSalesIntents);
    }
}
