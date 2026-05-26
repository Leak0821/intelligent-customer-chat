package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopChunkIndexWriter implements ChunkIndexWriter {
    @Override
    public void ensureIndex() {
    }

    @Override
    public int index(KnowledgeDocument document) {
        return 0;
    }
}
