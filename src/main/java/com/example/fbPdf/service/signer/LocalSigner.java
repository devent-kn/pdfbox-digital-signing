package com.example.fbPdf.service.signer;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.service.LocalSignService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

@Service
@RequiredArgsConstructor
public class LocalSigner implements PdfSigner {

    private final LocalSignService localSignService;

    @Override
    public byte[] sign(InputStream pdfInput) throws Exception {
        try (PDDocument document = PDDocument.load(pdfInput)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Demo User");
            signature.setLocation("VN");
            signature.setReason("Local signing");
            signature.setSignDate(Calendar.getInstance());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            document.addSignature(signature, localSignService);
            document.saveIncremental(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IOException("Signing error", e);
        }
    }

    @Override
    public SigningType getSigningType() {
        return SigningType.LOCAL;
    }
}
