package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.app.knowledge.KnowledgeImportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeFileTextExtractorTest {
    private final KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor();

    @Test
    void shouldExtractCsvIntoRowBlocks() {
        KnowledgeFileTextExtractor.ExtractedKnowledgeFile extracted = extractor.extract(
                new KnowledgeImportService.KnowledgeImportFile(
                        "faq.csv",
                        "text/csv",
                        """
                        question,answer
                        delivery,ships in 3 days
                        return,30-day refund
                        """.getBytes(StandardCharsets.UTF_8)
                )
        );

        assertThat(extracted.title()).isEqualTo("faq");
        assertThat(extracted.fileType()).isEqualTo("CSV");
        assertThat(extracted.content()).contains("记录 1");
        assertThat(extracted.content()).contains("question: delivery");
        assertThat(extracted.content()).contains("answer: 30-day refund");
    }

    @Test
    void shouldExtractDocxParagraphs() throws IOException {
        KnowledgeFileTextExtractor.ExtractedKnowledgeFile extracted = extractor.extract(
                new KnowledgeImportService.KnowledgeImportFile(
                        "policy.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        buildDocx()
                )
        );

        assertThat(extracted.fileType()).isEqualTo("WORD");
        assertThat(extracted.content()).contains("Return policy overview");
        assertThat(extracted.content()).contains("Please check the order number first.");
    }

    @Test
    void shouldExtractPdfText() throws IOException {
        KnowledgeFileTextExtractor.ExtractedKnowledgeFile extracted = extractor.extract(
                new KnowledgeImportService.KnowledgeImportFile(
                        "manual.pdf",
                        "application/pdf",
                        buildPdf()
                )
        );

        assertThat(extracted.fileType()).isEqualTo("PDF");
        assertThat(extracted.content()).contains("Knowledge import flow");
        assertThat(extracted.content()).contains("Keep parent and child documents linked");
    }

    private byte[] buildDocx() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph first = document.createParagraph();
            first.createRun().setText("Return policy overview");
            XWPFParagraph second = document.createParagraph();
            second.createRun().setText("Please check the order number first.");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] buildPdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(60, 720);
                contentStream.showText("Knowledge import flow");
                contentStream.newLineAtOffset(0, -18);
                contentStream.showText("Keep parent and child documents linked");
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
