package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.HybridRetrievalResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;

public interface HybridSearchService {
    HybridRetrievalResult search(RetrievalQuery query);
}
