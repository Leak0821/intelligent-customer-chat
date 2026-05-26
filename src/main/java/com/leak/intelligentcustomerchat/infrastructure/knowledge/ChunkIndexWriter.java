package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;

public interface ChunkIndexWriter {
    void ensureIndex();

    int index(KnowledgeDocument document);
}
