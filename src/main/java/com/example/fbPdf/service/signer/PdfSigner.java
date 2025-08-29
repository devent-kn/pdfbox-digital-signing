package com.example.fbPdf.service.signer;

import com.example.fbPdf.enums.SigningType;

import java.io.InputStream;

public interface PdfSigner {
    byte[] sign(InputStream pdfInput) throws Exception;
    SigningType getSigningType();
}
