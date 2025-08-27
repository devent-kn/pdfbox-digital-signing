package com.example.fbPdf.service;

import com.example.fbPdf.models.CMSProcessableInputStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;

public class LocalSignService implements SignatureInterface {

    private final PrivateKey privateKey;
    private final Certificate[] certificateChain;

    public LocalSignService(PrivateKey privateKey, Certificate[] certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            // Dùng BouncyCastle tạo chữ ký PKCS#7
            CMSProcessableInputStream cmsProcessable = new CMSProcessableInputStream(content);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                            .build(
                                    new JcaContentSignerBuilder("SHA256withRSA")
                                            .setProvider(new BouncyCastleProvider())
                                            .build(privateKey),
                                    (java.security.cert.X509Certificate) certificateChain[0]
                            )
            );

            Store certStore = new JcaCertStore(Arrays.asList(certificateChain));
            gen.addCertificates(certStore);

            CMSSignedData signedData = gen.generate(cmsProcessable, false);
            return signedData.getEncoded();

        } catch (Exception e) {
            throw new IOException("Signing error", e);
        }
    }
}
