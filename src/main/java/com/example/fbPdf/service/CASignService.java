package com.example.fbPdf.service;

import com.example.fbPdf.models.CMSProcessableInputStream;
import com.example.fbPdf.models.ExternalContentSigner;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class CASignService implements SignatureInterface {

    private final MockCA mockCA;

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            CMSProcessableInputStream cmsProcessable = new CMSProcessableInputStream(content);

            CMSSignedDataGenerator gen = getCmsSignedDataGenerator();
            CMSSignedData signedData = gen.generate(cmsProcessable, false);

            SignerInformation signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
            AttributeTable signedAttrs = signerInfo.getSignedAttributes();
            Attribute digestAttr = signedAttrs.get(CMSAttributes.messageDigest);
            byte[] digest = ((ASN1OctetString) digestAttr.getAttrValues().getObjectAt(0)).getOctets();
            System.out.println("pdfHash by BC " + Hex.toHexString(digest));

            return signedData.getEncoded();

        } catch (Exception e) {
            throw new IOException("Signing error", e);
        }
    }

    private CMSSignedDataGenerator getCmsSignedDataGenerator()
            throws CertificateEncodingException, IOException, OperatorCreationException, CMSException {
        ExternalContentSigner contentSigner = new ExternalContentSigner(mockCA);

        Certificate[] certificateChain = mockCA.getCertificateChain();
        JcaCertStore certStore = new JcaCertStore(Arrays.asList(certificateChain));
        X509CertificateHolder certHolder = new X509CertificateHolder(certificateChain[0].getEncoded());

        SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
        SignerInfoGenerator signerInfoGen = builder.build(contentSigner, certHolder);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoGen);
        gen.addCertificates(certStore);
        return gen;
    }
}
