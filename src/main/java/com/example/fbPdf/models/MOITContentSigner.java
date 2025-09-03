package com.example.fbPdf.models;

import com.example.fbPdf.service.MockCAService;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class MOITContentSigner implements ContentSigner {
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final MockCAService externalSignatureProvider;

    public MOITContentSigner(MockCAService externalSignatureProvider) {
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
    public byte[] getSignature() {
        byte[] toBeSigned = buffer.toByteArray();
        System.out.println("DER-encoded toBeSigned " + Hex.toHexString(toBeSigned));
        externalSignatureProvider.signHash(toBeSigned);
        return new byte[0];
    }
}
