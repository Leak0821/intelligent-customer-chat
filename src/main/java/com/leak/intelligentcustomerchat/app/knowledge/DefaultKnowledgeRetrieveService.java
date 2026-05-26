package com.leak.intelligentcustomerchat.app.knowledge;

import com.leak.intelligentcustomerchat.domain.business.BusinessFactResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentNormalizationResult;
import com.leak.intelligentcustomerchat.domain.intent.IntentRouteResult;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultKnowledgeRetrieveService implements KnowledgeRetrieveService {

    @Override
    public KnowledgeRetrieveResult retrieve(IntentNormalizationResult normalizationResult,
                                            IntentRouteResult routeResult,
                                            BusinessFactResult businessFactResult) {
        List<String> snippets = new ArrayList<>();
        snippets.add("knowledge placeholder for scene=" + routeResult.scene());
        snippets.add("sub-intent hint=" + routeResult.subIntent());
        if (!businessFactResult.facts().isEmpty()) {
            snippets.add("facts-first reminder");
        }
        return new KnowledgeRetrieveResult(snippets, snippets.size());
    }
}
