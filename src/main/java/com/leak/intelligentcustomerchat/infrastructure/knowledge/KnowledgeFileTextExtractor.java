package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeImportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class KnowledgeFileTextExtractor {

    public ExtractedKnowledgeFile extract(KnowledgeImportService.KnowledgeImportFile file) {
        if (file.content().length == 0) {
            throw new IllegalArgumentException("上传的知识文件为空: " + file.filename());
        }
        String extension = detectExtension(file.filename());
        try {
            return switch (extension) {
                case "csv" -> new ExtractedKnowledgeFile(baseName(file.filename()), extractCsv(file.content()), "CSV");
                case "docx" -> new ExtractedKnowledgeFile(baseName(file.filename()), extractDocx(file.content()), "WORD");
                case "doc" -> new ExtractedKnowledgeFile(baseName(file.filename()), extractDoc(file.content()), "WORD");
                case "pdf" -> new ExtractedKnowledgeFile(baseName(file.filename()), extractPdf(file.content()), "PDF");
                default -> throw new IllegalArgumentException("暂不支持的知识文件类型: " + file.filename());
            };
        } catch (IOException ex) {
            throw new IllegalArgumentException("解析知识文件失败: " + file.filename(), ex);
        }
    }

    private String extractCsv(byte[] content) throws IOException {
        List<List<String>> rows = parseCsv(content);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件没有可导入的内容");
        }
        List<String> firstRow = rows.get(0);
        boolean headerRow = rows.size() > 1 && looksLikeHeader(firstRow);
        List<String> headers = headerRow ? firstRow : defaultHeaders(columnCount(rows));
        int startIndex = headerRow ? 1 : 0;
        List<String> blocks = new ArrayList<>();
        for (int rowIndex = startIndex; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            List<String> fields = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String value = columnIndex < row.size() ? row.get(columnIndex) : "";
                if (value.isBlank()) {
                    continue;
                }
                fields.add(headers.get(columnIndex) + ": " + value);
            }
            if (!fields.isEmpty()) {
                blocks.add("记录 " + (rowIndex - startIndex + 1) + "\n" + String.join("\n", fields));
            }
        }
        return joinBlocks(blocks);
    }

    private List<List<String>> parseCsv(byte[] content) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            List<List<String>> rows = new ArrayList<>();
            List<String> currentRow = new ArrayList<>();
            StringBuilder currentCell = new StringBuilder();
            boolean inQuotes = false;
            int current;
            while ((current = reader.read()) != -1) {
                char ch = (char) current;
                if (ch == '"') {
                    if (inQuotes) {
                        reader.mark(1);
                        int next = reader.read();
                        if (next == '"') {
                            currentCell.append('"');
                        } else {
                            inQuotes = false;
                            if (next != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        inQuotes = true;
                    }
                    continue;
                }
                if (ch == ',' && !inQuotes) {
                    currentRow.add(normalizeCell(currentCell.toString()));
                    currentCell.setLength(0);
                    continue;
                }
                if ((ch == '\n' || ch == '\r') && !inQuotes) {
                    if (ch == '\r') {
                        reader.mark(1);
                        int next = reader.read();
                        if (next != '\n' && next != -1) {
                            reader.reset();
                        }
                    }
                    currentRow.add(normalizeCell(currentCell.toString()));
                    currentCell.setLength(0);
                    if (!isBlankRow(currentRow)) {
                        rows.add(List.copyOf(currentRow));
                    }
                    currentRow.clear();
                    continue;
                }
                currentCell.append(ch);
            }
            if (currentCell.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(normalizeCell(currentCell.toString()));
                if (!isBlankRow(currentRow)) {
                    rows.add(List.copyOf(currentRow));
                }
            }
            return rows;
        }
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(String::isBlank);
    }

    private boolean looksLikeHeader(List<String> row) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String cell : row) {
            if (cell.isBlank()) {
                return false;
            }
            normalized.add(cell.toLowerCase(Locale.ROOT));
        }
        return normalized.size() == row.size();
    }

    private int columnCount(List<List<String>> rows) {
        int max = 0;
        for (List<String> row : rows) {
            max = Math.max(max, row.size());
        }
        return Math.max(1, max);
    }

    private List<String> defaultHeaders(int columnCount) {
        List<String> headers = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            headers.add("列" + (index + 1));
        }
        return headers;
    }

    private String extractDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return normalizeFlowingText(extractor.getText());
        }
    }

    private String extractDoc(byte[] content) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(content));
             WordExtractor extractor = new WordExtractor(document)) {
            List<String> paragraphs = new ArrayList<>();
            for (String paragraph : extractor.getParagraphText()) {
                String normalized = normalizeInlineText(paragraph);
                if (!normalized.isBlank()) {
                    paragraphs.add(normalized);
                }
            }
            return joinBlocks(paragraphs);
        }
    }

    private String extractPdf(byte[] content) throws IOException {
        try (PDDocument document = PDDocument.load(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return normalizeFlowingText(stripper.getText(document));
        }
    }

    private String normalizeFlowingText(String rawText) {
        String normalized = rawText == null ? "" : rawText.replace("\u000c", "\n\n");
        List<String> blocks = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();
        for (String line : normalized.split("\\R")) {
            String trimmed = normalizeInlineText(line);
            if (trimmed.isBlank()) {
                flushBlock(blocks, currentBlock);
                continue;
            }
            if (currentBlock.length() > 0) {
                currentBlock.append(' ');
            }
            currentBlock.append(trimmed);
        }
        flushBlock(blocks, currentBlock);
        return joinBlocks(blocks);
    }

    private void flushBlock(List<String> blocks, StringBuilder currentBlock) {
        String normalized = normalizeInlineText(currentBlock.toString());
        if (!normalized.isBlank()) {
            blocks.add(normalized);
        }
        currentBlock.setLength(0);
    }

    private String joinBlocks(List<String> blocks) {
        List<String> normalized = blocks.stream()
                .map(this::normalizeInlineText)
                .filter(value -> !value.isBlank())
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("文件中没有可导入的文本内容");
        }
        return String.join("\n\n", normalized);
    }

    private String normalizeCell(String value) {
        String sanitized = value == null ? "" : value.replace("\uFEFF", "");
        return normalizeInlineText(sanitized);
    }

    private String normalizeInlineText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String detectExtension(String filename) {
        String normalized = filename == null ? "" : filename.trim().toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        return dotIndex >= 0 ? normalized.substring(dotIndex + 1) : "";
    }

    private String baseName(String filename) {
        String normalized = filename == null ? "" : filename.trim();
        int slashIndex = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        String shortName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        int dotIndex = shortName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return shortName.isBlank() ? "untitled" : shortName;
        }
        String title = shortName.substring(0, dotIndex).trim();
        return title.isBlank() ? "untitled" : title;
    }

    public record ExtractedKnowledgeFile(
            String title,
            String content,
            String fileType
    ) {
    }
}
