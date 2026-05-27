package com.leak.intelligentcustomerchat.infrastructure.config.runtime;

import com.leak.intelligentcustomerchat.domain.runtime.AgentRuntimeConfigSnapshot;
import com.leak.intelligentcustomerchat.domain.runtime.IntentCatalogConfig;
import com.leak.intelligentcustomerchat.domain.runtime.PromptSceneTemplateConfig;
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
                        You are an email customer support intent normalizer.
                        Rewrite noisy customer email content into a concise internal request without inventing facts.
                        Preserve explicit identifiers such as order numbers, tracking numbers, SKU, dates, quantities, and amounts.
                        Output JSON only with the following keys:
                        normalizedRequest, primaryQuestion, secondaryQuestions, sceneCandidates, subIntentCandidates, requiredEntities, missingEntities, disposition.
                        sceneCandidates must use PRE_SALES, AFTER_SALES, or UNKNOWN.
                        disposition must use CONTINUE, FOLLOW_UP, or HUMAN_REVIEW.
                        If the request is after-sales but lacks order number or tracking number, keep missingEntities containing order_id_or_tracking_no and disposition at least FOLLOW_UP.
                        """,
                        """
                        You are a senior email customer support agent.
                        Write a concise, courteous, production-style reply draft based only on the supplied facts, context summary, and knowledge snippets.
                        Do not invent refunds, shipping promises, dates, or policies that are not explicitly provided.
                        If the evidence is partial, say what is confirmed and keep the wording careful.
                        Return plain email body text only.
                        """,
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
                        "This draft is still running on the first production skeleton and will be refined with live tools, live facts, and RAG evidence in the next slices.",
                        new PromptSceneTemplateConfig(
                                java.util.Map.of(
                                        "PRE_SALES", """
                                                Hello,
                                                
                                                We have reviewed your question. To recommend a suitable next step, please share a little more detail about your intended use, preferred style, or target room setup.
                                                
                                                Current understanding:
                                                - Main request: {{primaryQuestion}}
                                                - Routed scene: {{scene}}
                                                
                                                Once we receive the missing detail, we will continue with a more precise recommendation.
                                                """,
                                        "AFTER_SALES", """
                                                Hello,
                                                
                                                We have reviewed your latest email. To continue, please share your order number or tracking number so we can verify the exact record for you.
                                                
                                                Current understanding:
                                                - Main request: {{primaryQuestion}}
                                                - Routed scene: {{scene}}
                                                
                                                Once we receive the missing information, we will continue with the next step.
                                                """,
                                        "UNKNOWN", """
                                                Hello,
                                                
                                                We have reviewed your latest email. To continue safely, please share the key order, product, or account detail related to your request.
                                                
                                                Current understanding:
                                                - Main request: {{primaryQuestion}}
                                                - Routed scene: {{scene}}
                                                
                                                Once we receive the missing information, we will continue with the next step.
                                                """
                                ),
                                java.util.Map.of(
                                        "PRE_SALES", """
                                                Hello,
                                                
                                                We have reviewed your request and a specialist is checking the recommendation boundary to avoid giving you an unsupported product or policy commitment.
                                                
                                                Current understanding:
                                                - Main request: {{primaryQuestion}}
                                                - Routed scene: {{scene}}
                                                
                                                We will follow up again after manual review.
                                                """,
                                        "AFTER_SALES", """
                                                Hello,
                                                
                                                We have reviewed your latest request and a specialist is checking the case to avoid giving you an incorrect commitment.
                                                
                                                Current understanding:
                                                - Main request: {{primaryQuestion}}
                                                - Routed scene: {{scene}}
                                                
                                                We will follow up again after manual review.
                                                """
                                ),
                                java.util.Map.of(
                                        "PRE_SALES", "This draft is still running on the first production skeleton and will be refined with richer catalog evidence and configurable recommendation prompts in the next slices.",
                                        "AFTER_SALES", "This draft is still running on the first production skeleton and will be refined with live order facts, logistics facts, and RAG evidence in the next slices."
                                )
                        )
                ),
                new IntentCatalogConfig(
                        List.of("product_recommendation", "product_comparison", "inventory_or_shipping", "general_inquiry"),
                        List.of("order_status", "logistics_tracking", "after_sales_policy", "return_refund", "general_inquiry")
                ),
                new RetrievalSettingsConfig(10, true, 50),
                "local-default",
                OffsetDateTime.now()
        );
    }
}
