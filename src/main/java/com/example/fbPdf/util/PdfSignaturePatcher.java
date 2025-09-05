package com.example.fbPdf.util;

import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;

@UtilityClass
public class PdfSignaturePatcher {

    public static void applySignature(File signaturePlaceholderPath, File signatureSignedPath, byte[] cmsSignature,
                                      String sigFieldName, int signatureContentOffset) throws IOException {
        try (FileInputStream fis = new FileInputStream(signaturePlaceholderPath);
             FileOutputStream fos = new FileOutputStream(signatureSignedPath)) {
            fis.transferTo(fos);
        }
        try (RandomAccessFile raf = new RandomAccessFile(signatureSignedPath, "rw");
             PDDocument document = Loader.loadPDF(signatureSignedPath)) {

            PDSignature signature = findExistingSignature(document, sigFieldName);
            COSDictionary sigDict = signature.getCOSObject();
            COSString contents = (COSString) sigDict.getDictionaryObject(COSName.CONTENTS);
            if (contents == null) {
                throw new IOException("No Contents entry in signature dictionary");
            }
            int reservedLen = contents.getBytes().length;

            String hexCms = Hex.toHexString(cmsSignature);
            if (hexCms.length() / 2 > reservedLen) {
                throw new IOException("CMS signature too large for reserved placeholder");
            }

            raf.seek(signatureContentOffset);
            raf.write(hexCms.getBytes());
        }
    }

    private PDSignature findExistingSignature(PDDocument document, String sigFieldName) {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }

        PDSignature signature = null;
        PDSignatureField signatureField = (PDSignatureField) acroForm.getField(sigFieldName);

        if (signatureField != null) {
            signature = signatureField.getSignature();
            if (signature == null) {
                signature = new PDSignature();
                signatureField.getCOSObject().setItem(COSName.V, signature);
            }
        }

        return signature;
    }
}
