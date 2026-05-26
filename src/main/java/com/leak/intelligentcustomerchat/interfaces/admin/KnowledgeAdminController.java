package com.leak.intelligentcustomerchat.interfaces.admin;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeRetrieveResult;
import com.leak.intelligentcustomerchat.domain.knowledge.RetrievalQuery;
import com.leak.intelligentcustomerchat.infrastructure.knowledge.KnowledgeRetriever;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeAdminController {
    private final KnowledgeRetriever knowledgeRetriever;

    public KnowledgeAdminController(KnowledgeRetriever knowledgeRetriever) {
        this.knowledgeRetriever = knowledgeRetriever;
    }

    @GetMapping("/search")
    public KnowledgeRetrieveResult search(@RequestParam String q,
                                          @RequestParam(defaultValue = "UNKNOWN") String scene,
                                          @RequestParam(defaultValue = "general_inquiry") String subIntent,
                                          @RequestParam(defaultValue = "10") int topK) {
        return knowledgeRetriever.retrieve(new RetrievalQuery(q, scene, subIntent, List.of(), topK));
    }
}
