package com.example.fbPdf.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

@Service
public class MockExternalSignatureProvider {

    @Autowired
    @Qualifier("caKeyStore")
    private KeyStore keyStore;

    @Value("${ca.keystore.alias}")
    private String alias;

    @Value("${ca.keystore.password}")
    private String keystorePassword;

    private PrivateKey getPrivateKey() throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
    }

    public Certificate[] getCertificateChain() throws Exception {
        return keyStore.getCertificateChain(alias);
    }

    public byte[] signHash(byte[] hash) throws Exception {
        PrivateKey privateKey = getPrivateKey();

        try {
            Signature signature = Signature.getInstance("NONEwithRSA");
            signature.initSign(privateKey);
            signature.update(hash);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Mock signing failed", e);
        }
    }
}