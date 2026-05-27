package com.leak.intelligentcustomerchat.infrastructure.knowledge;

public interface KnowledgeGovernanceStore {
    void validateImport(String knowledgeKey, String version, String contentHash);

    int deprecateOlderActiveVersions(String knowledgeKey, String currentVersion);
}
