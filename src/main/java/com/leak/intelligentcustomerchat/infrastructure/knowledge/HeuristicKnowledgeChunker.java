package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeChunk;
import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class HeuristicKnowledgeChunker implements KnowledgeChunker {
    private static final int TARGET_CHUNK_LENGTH = 420;
    private static final int MAX_CHUNK_LENGTH = 680;

    @Override
    public List<KnowledgeChunk> chunk(KnowledgeDocument document) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int chunkOrder = 0;
        for (String block : semanticBlocks(document.content())) {
            chunks.add(new KnowledgeChunk(
                    UUID.randomUUID().toString(),
                    document.documentId(),
                    document.title(),
                    block,
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

    private List<String> semanticBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        for (String paragraph : content.split("\\n\\s*\\n")) {
            String normalized = normalize(paragraph);
            if (normalized.isBlank()) {
                continue;
            }
            if (normalized.length() <= MAX_CHUNK_LENGTH) {
                blocks.add(normalized);
                continue;
            }
            blocks.addAll(splitLongParagraph(normalized));
        }
        return blocks;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> blocks = new ArrayList<>();
        String[] sentences = paragraph.split("(?<=[。！？!?；;\\.])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            String normalized = normalize(sentence);
            if (normalized.isBlank()) {
                continue;
            }
            if (normalized.length() > MAX_CHUNK_LENGTH) {
                flush(blocks, current);
                blocks.addAll(splitByLength(normalized));
                continue;
            }
            if (current.length() == 0) {
                current.append(normalized);
                continue;
            }
            if (current.length() + 1 + normalized.length() > TARGET_CHUNK_LENGTH) {
                flush(blocks, current);
                current.append(normalized);
                continue;
            }
            current.append(' ').append(normalized);
        }
        flush(blocks, current);
        return blocks;
    }

    private List<String> splitByLength(String paragraph) {
        List<String> blocks = new ArrayList<>();
        String remaining = paragraph;
        while (remaining.length() > MAX_CHUNK_LENGTH) {
            int splitAt = remaining.lastIndexOf(' ', TARGET_CHUNK_LENGTH);
            if (splitAt < TARGET_CHUNK_LENGTH / 2) {
                splitAt = MAX_CHUNK_LENGTH;
            }
            blocks.add(normalize(remaining.substring(0, splitAt)));
            remaining = normalize(remaining.substring(splitAt));
        }
        if (!remaining.isBlank()) {
            blocks.add(remaining);
        }
        return blocks;
    }

    private void flush(List<String> blocks, StringBuilder current) {
        String normalized = normalize(current.toString());
        if (!normalized.isBlank()) {
            blocks.add(normalized);
        }
        current.setLength(0);
    }

    private String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
