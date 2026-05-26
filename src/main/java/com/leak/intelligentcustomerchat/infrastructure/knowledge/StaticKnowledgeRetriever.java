package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeSnippet;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StaticKnowledgeRetriever implements KnowledgeRetriever {

    @Override
    public KnowledgeRetrieveResult retrieve(RetrievalQuery query) {
        List<KnowledgeSnippet> snippets = new ArrayList<>();
        snippets.add(new KnowledgeSnippet(
                UUID.randomUUID().toString(),
                "static knowledge for " + query.scene(),
                "Static fallback knowledge for subIntent=" + query.subIntent() + ", query=" + query.queryText(),
                1.0d,
                "static-knowledge-retriever"
        ));
        return new KnowledgeRetrieveResult("static-knowledge-retriever", snippets, snippets.size());
    }
}
