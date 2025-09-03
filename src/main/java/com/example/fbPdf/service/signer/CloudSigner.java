package com.example.fbPdf.service.signer;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.models.ExternalContentSigner;
import com.example.fbPdf.models.MOITContentSigner;
import com.example.fbPdf.service.CloudSignService;
import com.example.fbPdf.service.MockCAService;
import com.example.fbPdf.service.TSAClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudSigner implements PdfSigner {

    private final MockCAService mockCAService;
    private final CloudSignService cloudSignService;
    private final TSAClient tsaClient;

    public String createPlaceholder(String inputPath, String fieldName) throws Exception {
        byte[] contentToBeSigned;
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {

            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Pending Signer");
            signature.setReason("Async signing");
            signature.setSignDate(Calendar.getInstance());

//            PDSignatureField signatureField = new PDSignatureField(acroForm);
//            signatureField.setPartialName(fieldName);
//            signatureField.setValue(signature);
//
//            acroForm.getFields().add(signatureField);

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(8192 * 2);
            document.addSignature(signature, options);

            try (FileOutputStream fos = new FileOutputStream("pending.pdf")) {
                ExternalSigningSupport ext = document.saveIncrementalForExternalSigning(fos);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ext.getContent().transferTo(buffer);
                contentToBeSigned = buffer.toByteArray();

                try (FileOutputStream out = new FileOutputStream("contentToBeSigned.bin")) {
                    out.write(contentToBeSigned);
                }

                byte[] cmsSignature = generateMOITExternalSignature(contentToBeSigned);
                ext.setSignature(cmsSignature);
            }
//            document.save("pending.pdf");
        }

        return "pending.pdf";
    }


    public void signByMoit() throws Exception {
        byte[] contentToBeSigned;
        try (FileInputStream fis = new FileInputStream("contentToBeSigned.bin")) {
            contentToBeSigned = fis.readAllBytes();
        }

        // Tạo chữ ký CMS giả lập (mock)
        byte[] cmsSignature = generateExternalSignature(contentToBeSigned);
        fillSignature(
                "pending.pdf",
                "done.pdf",
                "Signature1",
                cmsSignature
        );
    }

    public void injectSignature(String placeholderPath, String signedPath, byte[] cmsSignature) throws Exception {
        File pendingFile = new File(placeholderPath);
        System.out.println("File size = " + pendingFile.length());
        try (FileOutputStream fos = new FileOutputStream(signedPath);
             PDDocument doc = Loader.loadPDF(pendingFile)) {

            // Tạo signature object để doc biết chỗ mà set chữ ký
            PDSignature signature = new PDSignature();
            doc.addSignature(signature);

            ExternalSigningSupport ext = doc.saveIncrementalForExternalSigning(fos);
            ext.setSignature(cmsSignature);
        }
    }

    public void fillSignature(String placeholderPdf, String signedPdf, String fieldName, byte[] cmsSignature) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(signedPdf);
             PDDocument doc = Loader.loadPDF(new File(placeholderPdf))) {

            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            PDSignatureField sigField = (PDSignatureField) acroForm.getField(fieldName);
            PDSignature sigDict = sigField.getSignature();

            COSString cosContents = (COSString) sigDict.getCOSObject().getDictionaryObject(COSName.CONTENTS);
            byte[] padded = new byte[cosContents.getBytes().length];
            System.arraycopy(cmsSignature, 0, padded, 0, cmsSignature.length);

            COSString newContents = new COSString(padded);
            sigDict.getCOSObject().setItem(COSName.CONTENTS, newContents);

            doc.saveIncremental(fos);
        }
    }


//        public void applyExternalSignature(String pendingFileName,
//                                       String outputFileName,
//                                       byte[] cmsSignature) throws Exception {
//        File pendingFile = new File(pendingFileName);
//        System.out.println("File size = " + pendingFile.length());
//        try (PDDocument document = Loader.loadPDF(pendingFile);
//            FileOutputStream fos = new FileOutputStream(outputFileName)) {
//            ExternalSigningSupport ext = document.saveIncrementalForExternalSigning(fos);
//            ext.setSignature(cmsSignature);
//        }
//    }




    // Cách 1: dùng saveIncrementalForExternalSigning()
    @Override
    public byte[] sign(InputStream pdfInput) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfInput.readAllBytes())) {
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
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            externalSigningSupport.getContent().transferTo(buffer);
            byte[] data = buffer.toByteArray();

            byte[] cmsSignature = generateExternalSignature(data);
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

    private byte[] generateExternalSignature(byte[] pdfByteRangeData) throws Exception {
        Certificate[] chain = mockCAService.getCertificateChain();
        JcaCertStore certs = new JcaCertStore(Arrays.asList(chain));

        ExternalContentSigner contentSigner = new ExternalContentSigner(mockCAService);

        X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
        SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
        SignerInfoGenerator signerInfoGen = builder.build(contentSigner, certHolder);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoGen);
        gen.addCertificates(certs);

        CMSTypedData cmsProcessable = new CMSProcessableByteArray(pdfByteRangeData);

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

    private byte[] generateMOITExternalSignature(byte[] pdfByteRangeData) throws Exception {
        Certificate[] chain = mockCAService.getCertificateChain();
        JcaCertStore certs = new JcaCertStore(Arrays.asList(chain));

        MOITContentSigner contentSigner = new MOITContentSigner(mockCAService);

        X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
        SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
        SignerInfoGenerator signerInfoGen = builder.build(contentSigner, certHolder);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoGen);
        gen.addCertificates(certs);

        CMSTypedData cmsProcessable = new CMSProcessableByteArray(pdfByteRangeData);

        CMSSignedData signedData = gen.generate(cmsProcessable, false);

        return signedData.getEncoded();
    }
}
