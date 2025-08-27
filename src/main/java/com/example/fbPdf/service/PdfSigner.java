package com.example.fbPdf.service;

import com.example.fbPdf.enums.SigningProviderType;

import java.io.InputStream;

public interface PdfSigner {
    byte[] sign(InputStream pdfInput) throws Exception;
    SigningProviderType getSigningType();
}
