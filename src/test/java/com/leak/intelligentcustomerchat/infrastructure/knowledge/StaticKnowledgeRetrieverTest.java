package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticKnowledgeRetrieverTest {
    private final StaticKnowledgeRetriever retriever = new StaticKnowledgeRetriever(
            new KnowledgeSeedCatalog(),
            new HeuristicKnowledgeChunker()
    );

    @Test
    void shouldReturnRelevantSeedSnippetForAfterSalesPolicy() {
        KnowledgeRetrieveResult result = retriever.retrieve(
                new RetrievalQuery("shipping policy", "AFTER_SALES", "after_sales_policy", List.of(), 5)
        );

        assertThat(result.source()).isEqualTo("static-knowledge-retriever");
        assertThat(result.recallCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.snippets()).anySatisfy(snippet -> {
            assertThat(snippet.title()).contains("售后政策");
            assertThat(snippet.content()).contains("标准政策");
        });
    }
}
