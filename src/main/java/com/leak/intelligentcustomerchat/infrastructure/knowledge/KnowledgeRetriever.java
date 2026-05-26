package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;

public interface KnowledgeRetriever {
    KnowledgeRetrieveResult retrieve(RetrievalQuery query);
}
