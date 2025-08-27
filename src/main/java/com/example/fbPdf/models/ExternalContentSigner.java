package com.example.fbPdf.models;

import com.example.fbPdf.service.MockExternalSignatureProvider;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

public class ExternalContentSigner implements ContentSigner {
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final MockExternalSignatureProvider externalSignatureProvider;

    public ExternalContentSigner(MockExternalSignatureProvider externalSignatureProvider) {
        this.externalSignatureProvider = externalSignatureProvider;
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption);
    }

    @Override
    public OutputStream getOutputStream() {
        return buffer;
    }

    @Override
    @SneakyThrows
    public byte[] getSignature() {
        byte[] toBeSigned = buffer.toByteArray();

        System.out.println("toBeSigned " + Hex.toHexString(toBeSigned));

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(toBeSigned);

        System.out.println("hash chữ ký " + Hex.toHexString(hash));

        return externalSignatureProvider.signHash(hash);
    }
}
