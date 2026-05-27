package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopKnowledgeGovernanceStore implements KnowledgeGovernanceStore {
    @Override
    public void validateImport(String knowledgeKey, String version, String contentHash) {
    }

    @Override
    public int deprecateOlderActiveVersions(String knowledgeKey, String currentVersion) {
        return 0;
    }
}
