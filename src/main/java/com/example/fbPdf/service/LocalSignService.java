package com.example.fbPdf.service;

import com.example.fbPdf.models.CMSProcessableInputStream;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;

@Service
public class LocalSignService implements SignatureInterface {

    @Autowired
    @Qualifier("localSignKeyStore")
    private KeyStore keyStore;

    @Value("${app.keystore.alias}")
    private String alias;

    @Value("${app.keystore.password}")
    private String keystorePassword;

    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    @PostConstruct
    private void init() throws Exception {
        this.privateKey = (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
        this.certificateChain = keyStore.getCertificateChain(alias);
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            CMSProcessableInputStream cmsProcessable = new CMSProcessableInputStream(content);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            JcaDigestCalculatorProviderBuilder digestCalculatorProviderBuilder
                    = new JcaDigestCalculatorProviderBuilder();
            JcaSignerInfoGeneratorBuilder signerInfoGeneratorBuilder
                    = new JcaSignerInfoGeneratorBuilder(digestCalculatorProviderBuilder.build());
            SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                    new JcaContentSignerBuilder("SHA256withRSA")
                            .setProvider(new BouncyCastleProvider())
                            .build(privateKey),
                    (java.security.cert.X509Certificate) certificateChain[0]
            );
            gen.addSignerInfoGenerator(signerInfoGenerator);

            JcaCertStore certStore = new JcaCertStore(Arrays.asList(certificateChain));
            gen.addCertificates(certStore);

            CMSSignedData signedData = gen.generate(cmsProcessable);
            return signedData.getEncoded();
        } catch (Exception e) {
            throw new IOException("Signing error", e);
        }
    }
}
