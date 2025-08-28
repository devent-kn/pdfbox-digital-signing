package com.example.fbPdf.factory;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.service.PdfSigner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfSignerFactory {

    private final Map<SigningType, PdfSigner> signerByType = new HashMap<>();

    public PdfSignerFactory(List<PdfSigner> signers) {
        for (PdfSigner s : signers) {
            signerByType.put(s.getSigningType(), s);
        }
    }

    public PdfSigner getPdfSigner(SigningType type) {
        if (type == null) {
            throw new IllegalArgumentException("Signer type must not be null");
        }
        PdfSigner signer = signerByType.get(type);
        if (signer == null) {
            throw new IllegalArgumentException("Unknown signer type: " + type);
        }
        return signer;
    }

}
