package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeChunk;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class HeuristicKnowledgeChunker implements KnowledgeChunker {

    @Override
    public List<KnowledgeChunk> chunk(KnowledgeDocument document) {
        String[] paragraphs = document.content().split("\\n\\s*\\n");
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int chunkOrder = 0;
        for (String paragraph : paragraphs) {
            String normalized = paragraph.replaceAll("\\s+", " ").trim();
            if (normalized.isBlank()) {
                continue;
            }
            chunks.add(new KnowledgeChunk(
                    UUID.randomUUID().toString(),
                    document.documentId(),
                    document.title(),
                    normalized,
                    chunkOrder++,
                    document.metadata()
            ));
        }
        if (chunks.isEmpty()) {
            chunks.add(new KnowledgeChunk(
                    UUID.randomUUID().toString(),
                    document.documentId(),
                    document.title(),
                    document.content().replaceAll("\\s+", " ").trim(),
                    0,
                    document.metadata()
            ));
        }
        return chunks;
    }
}
