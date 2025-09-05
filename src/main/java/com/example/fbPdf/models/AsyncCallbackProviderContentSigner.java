package com.example.fbPdf.models;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class AsyncCallbackProviderContentSigner implements ContentSigner {
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//    private final ExternalSignatureProvider externalSignatureProvider;

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
//        send content to be signed to external service and get signature by callback function
//        for eg: externalSignatureProvider.sign(toBeSigned);
        return new byte[0];
    }
}
