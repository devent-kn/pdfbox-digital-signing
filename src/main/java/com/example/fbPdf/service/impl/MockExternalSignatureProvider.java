package com.example.fbPdf.service.impl;

import jdk.jfr.Registered;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

@Service
@RequiredArgsConstructor
public class MockExternalSignatureProvider {

    private final KeyStore keyStore;

    @Value("${app.keystore.alias}")
    private String alias;

    @Value("${app.keystore.password}")
    private String keystorePassword;

    private PrivateKey getPrivateKey() throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
    }

    public Certificate[] getCertificateChain() throws Exception {
        return keyStore.getCertificateChain(alias);
    }

    public byte[] signHash(byte[] hash, String algorithm) throws Exception {
        PrivateKey privateKey = getPrivateKey();

        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(hash);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Mock signing failed", e);
        }
    }
}