package com.example.fbPdf.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD;
import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.TIMES_ROMAN;

@Service
public class PdfStyleService {

    public byte[] createStyledPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject image = PDImageXObject.createFromFile("image.jpg", document);

            try(PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font timesRomanFont = new PDType1Font(TIMES_ROMAN);
                PDType1Font helveticaBoldFont = new PDType1Font(HELVETICA_BOLD);

                // Font cơ bản
                contentStream.setFont(helveticaBoldFont, 18);
                contentStream.setNonStrokingColor(Color.BLUE);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Demo Apache PDFBox - Styling Text");
                contentStream.endText();

                // Font thường
                contentStream.setFont(timesRomanFont, 14);
                contentStream.setNonStrokingColor(Color.BLACK);

                // Left align
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("This is LEFT aligned text.");
                contentStream.endText();

                // Center align
                String centerText = "This is CENTER aligned text.";
                float stringWidth = timesRomanFont.getStringWidth(centerText) / 1000 * 14;
                float startX = (page.getMediaBox().getWidth() - stringWidth) / 2;
                contentStream.beginText();
                contentStream.setFont(timesRomanFont, 14);
                contentStream.newLineAtOffset(startX, 680);
                contentStream.showText(centerText);
                contentStream.endText();

                // Right align
                String rightText = "This is RIGHT aligned text.";
                stringWidth = timesRomanFont.getStringWidth(rightText) / 1000 * 14;
                startX = page.getMediaBox().getWidth() - stringWidth - 50;
                contentStream.beginText();
                contentStream.setFont(timesRomanFont, 14);
                contentStream.newLineAtOffset(startX, 660);
                contentStream.showText(rightText);
                contentStream.endText();

                // Underline (draw line manually)
                String underlineText = "This is UNDERLINED text.";
                contentStream.beginText();
                contentStream.setFont(helveticaBoldFont, 14);
                contentStream.newLineAtOffset(50, 630);
                contentStream.showText(underlineText);
                contentStream.endText();

                float underlineWidth = helveticaBoldFont.getStringWidth(underlineText) / 1000 * 14;
                contentStream.moveTo(50, 628);
                contentStream.lineTo(50 + underlineWidth, 628);
                contentStream.stroke();

                // Strike-through
                String strikeText = "This is STRIKETHROUGH text.";
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 600);
                contentStream.showText(strikeText);
                contentStream.endText();

                float strikeWidth = helveticaBoldFont.getStringWidth(strikeText) / 1000 * 14;
                contentStream.moveTo(50, 608);
                contentStream.lineTo(50 + strikeWidth, 608);
                contentStream.stroke();

                String longtext = "In the example provided in the previous chapter we discussed how to add text to a page in";
                contentStream.setFont(timesRomanFont, 14);
                contentStream.setLeading(14.5f);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 550);
                contentStream.showText(longtext);
                contentStream.newLine();
                contentStream.showText("a PDF but through this program, you can only add the text that would fit in a single line.");
                contentStream.newLine();
                contentStream.showText("If you try to add more content, all the text that exceeds the line space will not be displayed.");
                contentStream.endText();

                float scale = 0.5f;
                float imageWidth = page.getMediaBox().getWidth() - 100;
                float imageHeight = imageWidth / image.getWidth() * image.getHeight();
                contentStream.drawImage(image, 50, 470 - imageHeight * scale, imageWidth * scale, imageHeight * scale);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);

//            // Save to a file in current directory
//            String fileName = "output.txt";  // change name as needed
//            try (FileOutputStream fos = new FileOutputStream(fileName)) {
//                out.writeTo(fos);
//            }
            return out.toByteArray();
        }
    }
}
