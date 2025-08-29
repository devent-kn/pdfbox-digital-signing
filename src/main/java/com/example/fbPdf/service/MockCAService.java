package com.example.fbPdf.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

@Service
public class MockCAService {

    @Autowired
    @Qualifier("caKeyStore")
    private KeyStore keyStore;

    @Value("${ca.keystore.alias}")
    private String alias;

    @Value("${ca.keystore.password}")
    private String keystorePassword;

    private PrivateKey privateKey;

    @Getter
    private Certificate[] certificateChain;

    @PostConstruct
    private void init() throws Exception {
        this.privateKey = (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
        this.certificateChain = keyStore.getCertificateChain(alias);
    }

    public byte[] signHash(byte[] hash) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(hash);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Mock signing failed", e);
        }
    }
}