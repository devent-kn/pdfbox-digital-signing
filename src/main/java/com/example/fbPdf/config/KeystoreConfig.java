package com.example.fbPdf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

@Configuration
public class KeystoreConfig {

    @Value("${app.keystore.path}")
    private String keystorePath;

    @Value("${app.keystore.password}")
    private String keystorePassword;

    @Value("${ca.keystore.path}")
    private String caKeystorePath;

    @Value("${ca.keystore.password}")
    private String caKeystorePassword;

    @Value("${app.keystore.alias}")
    private String alias;

    @Bean("localSignKeyStore")
    public KeyStore localSignKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream is;
        String path = keystorePath.replace("classpath:", "");
        is = getClass().getResourceAsStream("/" + path);

        try (is) {
            keyStore.load(is, keystorePassword.toCharArray());
        }
        return keyStore;
    }

    @Bean("caKeyStore")
    public KeyStore caKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream is;
        String path = caKeystorePath.replace("classpath:", "");
        is = getClass().getResourceAsStream("/" + path);

        try (is) {
            keyStore.load(is, caKeystorePassword.toCharArray());
        }
        return keyStore;
    }

}
