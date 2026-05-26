package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticKnowledgeRetrieverTest {
    private final StaticKnowledgeRetriever retriever = new StaticKnowledgeRetriever();

    @Test
    void shouldReturnFallbackSnippet() {
        KnowledgeRetrieveResult result = retriever.retrieve(
                new RetrievalQuery("shipping policy", "AFTER_SALES", "after_sales_policy", List.of(), 5)
        );

        assertThat(result.source()).isEqualTo("static-knowledge-retriever");
        assertThat(result.recallCount()).isEqualTo(1);
        assertThat(result.snippets()).singleElement()
                .satisfies(snippet -> assertThat(snippet.content()).contains("after_sales_policy"));
    }
}
