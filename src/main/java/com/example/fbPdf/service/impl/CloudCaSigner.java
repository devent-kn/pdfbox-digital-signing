package com.example.fbPdf.service.impl;

import com.example.fbPdf.enums.SigningProviderType;
import com.example.fbPdf.service.PdfSigner;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

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

            document.addSignature(signature);

            ExternalSigningSupport externalSigningSupport = document.saveIncrementalForExternalSigning(baos);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = externalSigningSupport.getContent()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            byte[] pdfHash = digest.digest();

            // Build signed attributes (giống iText)
            ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
            // messageDigest
            signedAttrs.add(new Attribute(CMSAttributes.messageDigest, new DERSet(new DEROctetString(pdfHash))));
            // contentType = data
            signedAttrs.add(new Attribute(CMSAttributes.contentType, new DERSet(PKCSObjectIdentifiers.data)));
            // signingTime
            signedAttrs.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));
            // encode attributes
            DERSet signedAttrSet = new DERSet(signedAttrs);
            byte[] encodedSignedAttrs = new DERSequence(signedAttrSet).getEncoded("DER");

            // Hash attributes mock rồi nhờ CA/HSM ký
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] attrHash = sha256.digest(encodedSignedAttrs);
            byte[] signatureValue = mockExternalSignatureProvider.signHash(attrHash, "SHA256withRSA");
            Certificate[] chain = mockExternalSignatureProvider.getCertificateChain();

            // Build ContentSigner giả (trả chữ ký có sẵn)
            ContentSigner precomputed = new ContentSigner() {
                @Override
                public AlgorithmIdentifier getAlgorithmIdentifier() {
                    return new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption);
                }
                @Override
                public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
                @Override
                public byte[] getSignature() { return signatureValue; }
            };

            // SignerInfo với signed attributes
            X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
            SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
            builder.setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(new AttributeTable(signedAttrs)));

            SignerInfoGenerator signerInfoGen = builder.build(precomputed, certHolder);

            // Tạo CMS detached
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            gen.addSignerInfoGenerator(signerInfoGen);
            gen.addCertificates(new JcaCertStore(Arrays.asList(chain)));

            CMSSignedData signedData = gen.generate(new CMSAbsentContent(), false);
            byte[] cmsSignature = signedData.getEncoded();

            // Chèn chữ ký vào PDF
            externalSigningSupport.setSignature(cmsSignature);
            return baos.toByteArray();
        }
    }

    @Override
    public SigningProviderType getSigningType() {
        return SigningProviderType.CLOUD_CA;
    }
}
