package com.example.fbPdf.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Service
public class PdfService {

    public byte[] createStyledPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try(PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Font cơ bản
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.setNonStrokingColor(Color.BLUE);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 750);
                contentStream.showText("Demo Apache PDFBox - Styling Text");
                contentStream.endText();

                // Font thường
                contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);
                contentStream.setNonStrokingColor(Color.BLACK);

                // Left align
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("This is LEFT aligned text.");
                contentStream.endText();

                // Center align
                String centerText = "This is CENTER aligned text.";
                float stringWidth = PDType1Font.TIMES_ROMAN.getStringWidth(centerText) / 1000 * 14;
                float startX = (page.getMediaBox().getWidth() - stringWidth) / 2;
                contentStream.beginText();
                contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);
                contentStream.newLineAtOffset(startX, 680);
                contentStream.showText(centerText);
                contentStream.endText();

                // Right align
                String rightText = "This is RIGHT aligned text.";
                stringWidth = PDType1Font.TIMES_ROMAN.getStringWidth(rightText) / 1000 * 14;
                startX = page.getMediaBox().getWidth() - stringWidth - 50;
                contentStream.beginText();
                contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);
                contentStream.newLineAtOffset(startX, 660);
                contentStream.showText(rightText);
                contentStream.endText();

                // Underline (draw line manually)
                String underlineText = "This is UNDERLINED text.";
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, 630);
                contentStream.showText(underlineText);
                contentStream.endText();

                float underlineWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(underlineText) / 1000 * 14;
                contentStream.moveTo(50, 628);
                contentStream.lineTo(50 + underlineWidth, 628);
                contentStream.stroke();

                // Strike-through
                String strikeText = "This is STRIKETHROUGH text.";
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 600);
                contentStream.showText(strikeText);
                contentStream.endText();

                float strikeWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(strikeText) / 1000 * 14;
                contentStream.moveTo(50, 608);
                contentStream.lineTo(50 + strikeWidth, 608);
                contentStream.stroke();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
