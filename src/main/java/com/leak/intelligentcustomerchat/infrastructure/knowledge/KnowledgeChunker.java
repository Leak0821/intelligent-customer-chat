package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeChunk;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;

import java.util.List;

public interface KnowledgeChunker {
    List<KnowledgeChunk> chunk(KnowledgeDocument document);
}
