package com.example.fbPdf.util;

import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class PdfSignaturePatcher {

    public static void applySignature(File pendingPdf, File signedPdf, byte[] cmsSignature) throws IOException {
        try (FileInputStream fis = new FileInputStream(pendingPdf);
             FileOutputStream fos = new FileOutputStream(signedPdf)) {
            fis.transferTo(fos);
        }

        try (RandomAccessFile raf = new RandomAccessFile(signedPdf, "rw");
             PDDocument doc = Loader.loadPDF(signedPdf)) {

            PDSignature sig = doc.getLastSignatureDictionary();
            if (sig == null) {
                throw new IOException("No signature dictionary found in PDF");
            }

            int[] byteRange = sig.getByteRange();
            if (byteRange == null || byteRange.length != 4) {
                throw new IOException("Invalid ByteRange in signature dictionary");
            }

            COSDictionary sigDict = sig.getCOSObject();
            COSString contents = (COSString) sigDict.getDictionaryObject(COSName.CONTENTS);
            if (contents == null) {
                throw new IOException("No Contents entry in signature dictionary");
            }

            int reservedLen = contents.getBytes().length;
            int contentsOffset = findSignatureContentsOffset(raf, reservedLen);

            String hexCms = Hex.toHexString(cmsSignature);
            if (hexCms.length() / 2 > reservedLen) {
                throw new IOException("CMS signature too large for reserved placeholder");
            }
            StringBuilder paddedHex = new StringBuilder(hexCms);
            while (paddedHex.length() / 2 < reservedLen) {
                paddedHex.append("00");
            }

            raf.seek(contentsOffset);
            raf.write(paddedHex.toString().getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static int findSignatureContentsOffset(RandomAccessFile raf, int reservedLen) throws IOException {
        raf.seek(0);
        byte[] buffer = new byte[(int) raf.length()];
        raf.readFully(buffer);
        String pdf = new String(buffer, StandardCharsets.ISO_8859_1);

        // Tìm pattern "/Contents <"
        int idx = pdf.indexOf("/Contents <");
        if (idx < 0) {
            throw new IOException("Cannot find /Contents in PDF");
        }
        int start = idx + "/Contents <".length();

        return start; // vị trí trong file
    }
}
