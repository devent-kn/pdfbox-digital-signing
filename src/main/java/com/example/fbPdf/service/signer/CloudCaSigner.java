package com.example.fbPdf.service.signer;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.models.ExternalContentSigner;
import com.example.fbPdf.service.CASignService;
import com.example.fbPdf.service.MockCAService;
import com.example.fbPdf.service.TSAClient;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Calendar;

@Service
@RequiredArgsConstructor
public class CloudCaSigner implements PdfSigner {

    private final MockCAService mockCAService;
    private final CASignService caSignService;
    private final TSAClient tsaClient;

    // Cách 1: dùng saveIncrementalForExternalSigning()
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

//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            try (InputStream is = externalSigningSupport.getContent()) {
//                byte[] buffer = new byte[8192];
//                int read;
//                while ((read = is.read(buffer)) != -1) {
//                    md.update(buffer, 0, read);
//                }
//            }
//            byte[] pdfHashManual = md.digest();
//            System.out.println("pdfHashManual " + Hex.toHexString(pdfHashManual));

            byte[] cmsSignature = getExternalSignature(externalSigningSupport.getContent());
            externalSigningSupport.setSignature(cmsSignature);

            return baos.toByteArray();
        }
    }

    // Cách 2: dùng saveIncremental() và implement SignatureInterface
//    @Override
//    public byte[] sign(InputStream pdfInput) throws Exception {
//        try (PDDocument document = PDDocument.load(pdfInput)) {
//            PDSignature signature = new PDSignature();
//            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
//            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
//            signature.setName("Demo User");
//            signature.setLocation("VN");
//            signature.setReason("External signing");
//            signature.setSignDate(Calendar.getInstance());
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//            SignatureOptions options = new SignatureOptions();
//            options.setPreferredSignatureSize(8192 * 2);
//            document.addSignature(signature, caSignService, options);
//            document.saveIncremental(baos);
//            return baos.toByteArray();
//        }
//    }

    @Override
    public SigningType getSigningType() {
        return SigningType.CLOUD_CA;
    }

    private byte[] getExternalSignature(InputStream pdfByteRangeStream) throws Exception {
        Certificate[] chain = mockCAService.getCertificateChain();
        JcaCertStore certs = new JcaCertStore(Arrays.asList(chain));

        ExternalContentSigner contentSigner = new ExternalContentSigner(mockCAService);

        X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
        SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
        SignerInfoGenerator signerInfoGen = builder.build(contentSigner, certHolder);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoGen);
        gen.addCertificates(certs);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        pdfByteRangeStream.transferTo(buffer);
        byte[] data = buffer.toByteArray();
        CMSTypedData cmsProcessable = new CMSProcessableByteArray(data);

        CMSSignedData signedData = gen.generate(cmsProcessable, false);

//        SignerInformation signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
//        AttributeTable signedAttrs = signerInfo.getSignedAttributes();
//        Attribute digestAttr = signedAttrs.get(CMSAttributes.messageDigest);
//        byte[] digest = ((ASN1OctetString) digestAttr.getAttrValues().getObjectAt(0)).getOctets();
//        System.out.println("pdfHash by BC " + Hex.toHexString(digest));
//        byte[] signature = signerInfo.getSignature();
//        System.out.println("signature " + Hex.toHexString(signature));
//        byte[] derEncoded = signerInfo.getEncodedSignedAttributes();
//        System.out.println("DER-encoded signedAttributes: " + Hex.toHexString(derEncoded));

        signedData = tsaClient.addTimestamp(signedData);

        return signedData.getEncoded();
    }
}
