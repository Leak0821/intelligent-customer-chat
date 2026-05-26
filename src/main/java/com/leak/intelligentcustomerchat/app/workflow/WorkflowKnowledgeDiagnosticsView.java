package com.leak.intelligentcustomerchat.app.workflow;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.domain.runtime.RetrievalSettingsConfig;

import java.util.List;
import java.util.Objects;

public record WorkflowKnowledgeDiagnosticsView(
        RetrievalQuery retrievalQuery,
        RetrievalSettingsConfig retrievalSettings,
        String retrievalSource,
        String fusionStrategy,
        int recallCount,
        List<String> fusedSnippetIds,
        boolean hybridDebugAvailable,
        List<KnowledgeSnippet> bm25Snippets,
        List<KnowledgeSnippet> vectorSnippets
) {
    public WorkflowKnowledgeDiagnosticsView {
        Objects.requireNonNull(retrievalQuery, "retrievalQuery must not be null");
        Objects.requireNonNull(retrievalSettings, "retrievalSettings must not be null");
        Objects.requireNonNull(retrievalSource, "retrievalSource must not be null");
        Objects.requireNonNull(fusionStrategy, "fusionStrategy must not be null");
        fusedSnippetIds = List.copyOf(fusedSnippetIds);
        bm25Snippets = List.copyOf(bm25Snippets);
        vectorSnippets = List.copyOf(vectorSnippets);
    }
}
