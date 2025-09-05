package com.example.fbPdf.service;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.factory.PdfSignerFactory;
import com.example.fbPdf.service.signer.ExternalSigner;
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
    private final ExternalSigner externalSigner;

    public byte[] signPdf(File inputFile, SigningType signingType) throws Exception {
        PdfSigner pdfSigner = pdfSignerFactory.getPdfSigner(signingType);
        return pdfSigner.sign(inputFile, false);
    }

    public void preparePdfForExternalSigning(File inputFile) throws Exception {
        externalSigner.sign(inputFile, true);
    }

    public void callbackAsyncSignature(byte[] cmsSignature) throws Exception {
        externalSigner.callbackAsyncSignature(cmsSignature);
    }
}
