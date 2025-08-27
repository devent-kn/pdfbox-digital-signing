package com.example.fbPdf.service;

import com.example.fbPdf.enums.SigningProviderType;
import com.example.fbPdf.models.CMSProcessableInputStream;
import com.example.fbPdf.models.ExternalContentSigner;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CloudCaSigner implements PdfSigner {

    private final MockExternalSignatureProvider mockExternalSignatureProvider;

    @Override
    public byte[] sign(InputStream pdfInput) throws Exception {
        try (PDDocument document = PDDocument.load(pdfInput)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Demo User");
            signature.setLocation("VN");
            signature.setReason("External signing");
            signature.setSignDate(Calendar.getInstance());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(8192 * 2);
            document.addSignature(signature, options);

            ExternalSigningSupport externalSigningSupport = document.saveIncrementalForExternalSigning(baos);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = externalSigningSupport.getContent()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            byte[] pdfHashManual = md.digest();
            System.out.println("pdfHash by BC " + Hex.toHexString(pdfHashManual));
//
//            Certificate[] chain = mockExternalSignatureProvider.getCertificateChain();
//            ContentSigner signer = new ExternalContentSigner(mockExternalSignatureProvider);
//
//            X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
//            SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
//            SignerInfoGenerator signerInfoGen = builder.build(signer, certHolder);
//
//            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
//            gen.addSignerInfoGenerator(signerInfoGen);
//            gen.addCertificates(new JcaCertStore(Arrays.asList(chain)));
//
//            CMSSignedData signedData = gen.generate(new CMSAbsentContent(), false);
//            byte[] cmsSignature = signedData.getEncoded();
//
//            SignerInformation signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
//            AttributeTable signedAttrs = signerInfo.getSignedAttributes();
//            Attribute digestAttr = signedAttrs.get(CMSAttributes.messageDigest);
//            byte[] digest = ((ASN1OctetString) digestAttr.getAttrValues().getObjectAt(0)).getOctets();
//            System.out.println("pdfHash by BC " + Hex.toHexString(digest));
//
//            if (Arrays.equals(pdfHashManual, digest)) {
//                System.out.println("OK: Hashes match");
//            } else {
//                System.out.println("Mismatch!");
//            }

            byte[] cmsSignature = createCmsWithRemoteSignature(externalSigningSupport.getContent());

            externalSigningSupport.setSignature(cmsSignature);
            return baos.toByteArray();
        }
    }

    @Override
    public SigningProviderType getSigningType() {
        return SigningProviderType.CLOUD_CA;
    }

    private byte[] createCmsWithRemoteSignature(InputStream pdfByteRangeStream) throws Exception {
        Certificate[] chain = mockExternalSignatureProvider.getCertificateChain();
        X509Certificate signerCert = (X509Certificate) chain[0];

        List<Certificate> certList = Arrays.asList(chain);
        JcaCertStore certs = new JcaCertStore(certList);

        ExternalContentSigner contentSigner = new ExternalContentSigner(mockExternalSignatureProvider);

        // Cach 1
        X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
        SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
        SignerInfoGenerator signerInfoGen = builder.build(contentSigner, certHolder);

        // Cach 2
//        DigestCalculatorProvider dcp =
//                new JcaDigestCalculatorProviderBuilder().setProvider(new BouncyCastleProvider()).build();
//        JcaSignerInfoGeneratorBuilder sigInfoBuilder = new JcaSignerInfoGeneratorBuilder(dcp);
//        SignerInfoGenerator signerInfoGen = sigInfoBuilder.build(contentSigner, signerCert);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoGen);
        gen.addCertificates(certs);

        CMSTypedData msg = new CMSProcessableInputStream(pdfByteRangeStream);
        CMSSignedData signedData = gen.generate(msg, false);

        SignerInformation signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
        AttributeTable signedAttrs = signerInfo.getSignedAttributes();
        Attribute digestAttr = signedAttrs.get(CMSAttributes.messageDigest);
        byte[] digest = ((ASN1OctetString) digestAttr.getAttrValues().getObjectAt(0)).getOctets();
        System.out.println("pdfHash by BC " + Hex.toHexString(digest));

        byte[] signature = signerInfo.getSignature();
        return signedData.getEncoded();
    }
}
