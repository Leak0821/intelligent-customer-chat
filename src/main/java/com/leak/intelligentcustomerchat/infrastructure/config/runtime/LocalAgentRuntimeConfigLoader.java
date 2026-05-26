package com.leak.intelligentcustomerchat.infrastructure.config.runtime;

import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptTemplateConfig;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class LocalAgentRuntimeConfigLoader implements AgentRuntimeConfigLoader {
    @Override
    public AgentRuntimeConfigSnapshot load() {
        return new AgentRuntimeConfigSnapshot(
                new PromptTemplateConfig(
                        """
                        Hello,
                        
                        We have reviewed your latest email. To continue, please share your order number or tracking number so we can verify the exact record for you.
                        
                        Current understanding:
                        - Main request: {{primaryQuestion}}
                        - Routed scene: {{scene}}
                        
                        Once we receive the missing information, we will continue with the next step.
                        """,
                        """
                        Hello,
                        
                        We have reviewed your latest request and a specialist is checking the case to avoid giving you an incorrect commitment.
                        
                        Current understanding:
                        - Main request: {{primaryQuestion}}
                        - Routed scene: {{scene}}
                        
                        We will follow up again after manual review.
                        """,
                        "This draft is still running on the first production skeleton and will be refined with live tools, live facts, and RAG evidence in the next slices."
                ),
                new IntentCatalogConfig(
                        List.of("product_recommendation", "product_comparison", "inventory_or_shipping", "general_inquiry"),
                        List.of("order_status", "logistics_tracking", "after_sales_policy", "general_inquiry")
                ),
                new RetrievalSettingsConfig(10, true, 50),
                "local-default",
                OffsetDateTime.now()
        );
    }
}
