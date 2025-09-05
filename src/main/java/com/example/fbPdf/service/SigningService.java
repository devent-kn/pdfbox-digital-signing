package com.example.fbPdf.service;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.factory.PdfSignerFactory;
import com.example.fbPdf.service.signer.CloudSigner;
import com.example.fbPdf.service.signer.PdfSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class SigningService {

    private final PdfSignerFactory pdfSignerFactory;
    private final CloudSigner cloudSigner;

    public byte[] signPdf(File inputPdf, SigningType signingType) throws Exception {
        PdfSigner pdfSigner = pdfSignerFactory.getPdfSigner(signingType);
        return pdfSigner.sign(inputPdf);
    }

    public void preparePdfForExternalSigning(String fileName) throws Exception {
        cloudSigner.createSignaturePlaceholder(fileName);
    }

    public void callbackAsyncSignature(byte[] cmsSignature) throws Exception {
        cloudSigner.callbackAsyncSignature(cmsSignature);
    }
}
