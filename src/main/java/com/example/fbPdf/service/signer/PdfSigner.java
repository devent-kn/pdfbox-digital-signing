package com.example.fbPdf.service.signer;

import com.example.fbPdf.enums.SigningType;

import java.io.File;

public interface PdfSigner {
    byte[] sign(File fileInput, boolean async) throws Exception;
    SigningType getSigningType();
}
