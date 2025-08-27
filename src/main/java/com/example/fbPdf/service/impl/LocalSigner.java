package com.example.fbPdf.service.impl;

import com.example.fbPdf.enums.SigningProviderType;
import com.example.fbPdf.service.LocalSignService;
import com.example.fbPdf.service.PdfSigner;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Calendar;

@Service
@RequiredArgsConstructor
public class LocalSigner implements PdfSigner {

    private final KeyStore keyStore;

    @Value("${app.keystore.alias}")
    private String alias;

    @Value("${app.keystore.password}")
    private String keystorePassword;

    private PrivateKey getPrivateKey() throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
    }

    private Certificate[] getCertificateChain() throws Exception {
        return keyStore.getCertificateChain(alias);
    }

    @Override
    public byte[] sign(InputStream pdfInput) throws Exception {
        PrivateKey privateKey = getPrivateKey();
        Certificate[] certificateChain = getCertificateChain();

        try (PDDocument document = PDDocument.load(pdfInput)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Demo User");
            signature.setLocation("VN");
            signature.setReason("Local signing");
            signature.setSignDate(Calendar.getInstance());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            document.addSignature(signature, new LocalSignService(privateKey, certificateChain));
            document.saveIncremental(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IOException("Signing error", e);
        }
    }

    @Override
    public SigningProviderType getSigningType() {
        return SigningProviderType.LOCAL;
    }
}
