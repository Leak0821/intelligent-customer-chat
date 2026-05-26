package com.leak.intelligentcustomerchat.app.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leak.intelligentcustomerchat.app.ai.LlmClient;
import com.leak.intelligentcustomerchat.app.config.PromptConfigService;
import com.leak.intelligentcustomerchat.domain.intent.CustomerScene;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.ProcessingDisposition;
import com.leak.intelligentcustomerchat.domain.mail.InboundMail;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DefaultIntentNormalizationService implements IntentNormalizationService {
    private final PromptConfigService promptConfigService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final IntentNormalizationHeuristics heuristics = new IntentNormalizationHeuristics();

    public DefaultIntentNormalizationService(PromptConfigService promptConfigService,
                                             LlmClient llmClient,
                                             ObjectMapper objectMapper) {
        this.promptConfigService = promptConfigService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntentNormalizationResult normalize(InboundMail mail) {
        IntentNormalizationResult heuristicResult = heuristics.normalize(mail);
        String userPrompt = buildUserPrompt(mail, heuristicResult);
        return llmClient.complete(promptConfigService.currentPromptConfig().intentNormalizationSystemPrompt(), userPrompt)
                .flatMap(this::parseLlmResponse)
                .map(response -> mergeWithGuardrails(mail, heuristicResult, response))
                .orElse(heuristicResult);
    }

    private String buildUserPrompt(InboundMail mail, IntentNormalizationResult heuristicResult) {
        return """
                Subject:
                %s

                Body:
                %s

                Heuristic baseline:
                - sceneCandidates=%s
                - subIntentCandidates=%s
                - requiredEntities=%s
                - missingEntities=%s
                - disposition=%s
                """.formatted(
                mail.subject(),
                mail.rawBody(),
                heuristicResult.sceneCandidates(),
                heuristicResult.subIntentCandidates(),
                heuristicResult.requiredEntities(),
                heuristicResult.missingEntities(),
                heuristicResult.disposition()
        );
    }

    private java.util.Optional<LlmIntentNormalizationPayload> parseLlmResponse(String content) {
        try {
            return java.util.Optional.of(objectMapper.readValue(content, LlmIntentNormalizationPayload.class));
        } catch (Exception ignored) {
            // 模型未按 JSON 合同输出时，直接回退启发式路径，避免主链路被不稳定输出拖垮。
            return java.util.Optional.empty();
        }
    }

    private IntentNormalizationResult mergeWithGuardrails(InboundMail mail,
                                                          IntentNormalizationResult heuristicResult,
                                                          LlmIntentNormalizationPayload llmPayload) {
        String mergedMailText = heuristics.mergeMailText(mail.subject(), mail.rawBody());
        List<CustomerScene> sceneCandidates = normalizeScenes(llmPayload.sceneCandidates(), heuristicResult.sceneCandidates());
        List<String> subIntentCandidates = normalizeStrings(llmPayload.subIntentCandidates(), heuristicResult.subIntentCandidates());
        List<String> secondaryQuestions = normalizeStrings(llmPayload.secondaryQuestions(), List.of());
        List<String> requiredEntities = normalizeStrings(llmPayload.requiredEntities(), heuristicResult.requiredEntities());
        List<String> missingEntities = normalizeStrings(llmPayload.missingEntities(), heuristicResult.missingEntities());
        ProcessingDisposition disposition = mergeDisposition(llmPayload.disposition(), heuristicResult.disposition());

        // 订单号、物流号这类关键实体不能靠模型“猜有”。如果规则层没有识别到，就维持追问或更高等级处置。
        boolean hasExplicitOrderId = heuristics.hasExplicitOrderOrTrackingId(mergedMailText);
        boolean afterSalesLike = sceneCandidates.contains(CustomerScene.AFTER_SALES)
                || heuristicResult.sceneCandidates().contains(CustomerScene.AFTER_SALES);
        if (afterSalesLike && !hasExplicitOrderId) {
            requiredEntities = mergeDistinct(requiredEntities, List.of(IntentNormalizationHeuristics.ORDER_ID_ENTITY));
            missingEntities = mergeDistinct(missingEntities, List.of(IntentNormalizationHeuristics.ORDER_ID_ENTITY));
            disposition = mergeDisposition(ProcessingDisposition.FOLLOW_UP.name(), disposition);
        }

        return new IntentNormalizationResult(
                firstNonBlank(llmPayload.normalizedRequest(), heuristicResult.normalizedRequest()),
                firstNonBlank(llmPayload.primaryQuestion(), heuristicResult.primaryQuestion()),
                secondaryQuestions,
                sceneCandidates,
                subIntentCandidates,
                requiredEntities,
                missingEntities,
                disposition
        );
    }

    private List<CustomerScene> normalizeScenes(List<String> values, List<CustomerScene> fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        List<CustomerScene> normalized = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                normalized.add(CustomerScene.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // 忽略不在合同内的场景枚举，避免脏数据污染主路由。
            }
        }
        return normalized.isEmpty() ? fallback : deduplicateScenes(normalized);
    }

    private List<String> normalizeStrings(List<String> values, List<String> fallback) {
        if (values == null) {
            return fallback;
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            normalized.add(value.trim());
        }
        return normalized.isEmpty() ? fallback : List.copyOf(normalized);
    }

    private ProcessingDisposition mergeDisposition(String llmDisposition, ProcessingDisposition fallback) {
        if (llmDisposition == null || llmDisposition.isBlank()) {
            return fallback;
        }
        try {
            return maxDisposition(ProcessingDisposition.valueOf(llmDisposition.trim().toUpperCase(Locale.ROOT)), fallback);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private ProcessingDisposition maxDisposition(ProcessingDisposition left, ProcessingDisposition right) {
        return severity(left) >= severity(right) ? left : right;
    }

    private int severity(ProcessingDisposition disposition) {
        return switch (disposition) {
            case CONTINUE -> 0;
            case FOLLOW_UP -> 1;
            case HUMAN_REVIEW -> 2;
        };
    }

    private List<String> mergeDistinct(List<String> left, List<String> right) {
        Set<String> merged = new LinkedHashSet<>(left);
        merged.addAll(right);
        return List.copyOf(merged);
    }

    private List<CustomerScene> deduplicateScenes(List<CustomerScene> scenes) {
        return List.copyOf(new LinkedHashSet<>(scenes));
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary.trim();
    }

    private record LlmIntentNormalizationPayload(
            String normalizedRequest,
            String primaryQuestion,
            List<String> secondaryQuestions,
            List<String> sceneCandidates,
            List<String> subIntentCandidates,
            List<String> requiredEntities,
            List<String> missingEntities,
            String disposition
    ) {
    }
}
