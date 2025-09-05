package com.example.fbPdf.service.signer;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.models.ExternalContentSigner;
import com.example.fbPdf.models.AsyncCallbackProviderContentSigner;
import com.example.fbPdf.service.CloudSignService;
import com.example.fbPdf.service.MockCAService;
import com.example.fbPdf.service.TSAClient;
import com.example.fbPdf.util.PdfSignaturePatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    private int signatureContentOffset;
    private final String signatureFieldName = "SignatureTA";
    private static final String SIGNATURE_PLACEHOLDER_PATH = "files/signature-placeholder.pdf";
    private static final String SIGNATURE_CALLBACK_SIGNED_PATH = "files/signature-callback-signed.pdf";
    private static final String SIGNATURE_SIGNED_PATH = "files/signature-signed.pdf";

    // Cách 1: dùng saveIncrementalForExternalSigning()
    @Override
    public byte[] sign(File fileInput) throws Exception {
        File signatureSignedFile = new File(SIGNATURE_SIGNED_PATH);
        try (PDDocument document = Loader.loadPDF(fileInput)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Demo User");
            signature.setLocation("VN");
            signature.setReason("External signing");
            signature.setSignDate(Calendar.getInstance());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(8192);
            document.addSignature(signature, options);

            ExternalSigningSupport externalSigningSupport = document.saveIncrementalForExternalSigning(baos);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            externalSigningSupport.getContent().transferTo(buffer);
            byte[] contentToBeSigned = buffer.toByteArray();

            byte[] cmsSignature = signByExternalProvider(contentToBeSigned);
            externalSigningSupport.setSignature(cmsSignature);

            try (FileOutputStream fos = new FileOutputStream(signatureSignedFile)) {
                fos.write(baos.toByteArray());
            }
            return baos.toByteArray();
        }
    }

    // Cách 2: dùng saveIncremental() và implement SignatureInterface
//    @Override
//    public byte[] sign(File pdfInput) throws Exception {
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

    public void createSignaturePlaceholder(String inputPath) throws Exception {
        byte[] contentToBeSigned;
        File signaturePlaceholderFile = new File(SIGNATURE_PLACEHOLDER_PATH);
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {

            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Callback Signature Signer");
            signature.setReason("Async signing");
            signature.setSignDate(Calendar.getInstance());

            // Customize the signature field name
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName(signatureFieldName);
            signatureField.setValue(signature);

            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            PDPage page1 = document.getPage(0);
            widget.setPage(page1);
            page1.getAnnotations().add(widget);

            acroForm.getFields().add(signatureField);

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(8192*2);
            document.addSignature(signature, options);

            try (FileOutputStream fos = new FileOutputStream(signaturePlaceholderFile)) {
                ExternalSigningSupport signingSupport = document.saveIncrementalForExternalSigning(fos);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                signingSupport.getContent().transferTo(buffer);
                contentToBeSigned = buffer.toByteArray();

                signByAsyncExternalProvider(contentToBeSigned);
                signatureContentOffset = signature.getByteRange()[1] + 1;
                signingSupport.setSignature(new byte[0]);

                // save contentToBeSigned to file for mocking external signature callback purpose
                try (FileOutputStream out = new FileOutputStream("files/contentToBeSigned.bin")) {
                    out.write(contentToBeSigned);
                }
            }
        }
    }

    public void callbackAsyncSignature(byte[] cmsSignature) throws Exception {
        applyExternalSignature(
                SIGNATURE_PLACEHOLDER_PATH,
                SIGNATURE_CALLBACK_SIGNED_PATH,
                cmsSignature,
                signatureFieldName,
                signatureContentOffset
        );
    }

    private void applyExternalSignature(String signaturePlaceholderPath, String signatureSignedPath,
                                        byte[] callbackSignature, String fieldName, int signatureContentOffset) throws Exception {
        File pendingFile = new File(signaturePlaceholderPath);
        File signedFile = new File(signatureSignedPath);
        PdfSignaturePatcher.applySignature(pendingFile, signedFile, callbackSignature, fieldName, signatureContentOffset);
    }

    public byte[] signByExternalProvider(byte[] pdfByteRangeData) throws Exception {
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

//        signedData = tsaClient.addTimestamp(signedData);

        return signedData.getEncoded();
    }

    private void signByAsyncExternalProvider(byte[] pdfByteRangeData) throws Exception {
        Certificate[] chain = mockCAService.getCertificateChain();
        JcaCertStore certs = new JcaCertStore(Arrays.asList(chain));

        AsyncCallbackProviderContentSigner contentSigner = new AsyncCallbackProviderContentSigner();

        X509CertificateHolder certHolder = new X509CertificateHolder(chain[0].getEncoded());
        SignerInfoGeneratorBuilder builder = new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider());
        SignerInfoGenerator signerInfoGen = builder.build(contentSigner, certHolder);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(signerInfoGen);
        gen.addCertificates(certs);

        CMSTypedData cmsProcessable = new CMSProcessableByteArray(pdfByteRangeData);

        gen.generate(cmsProcessable, false);
    }

    // to debug to view pdf components
    public static void main(String[] args) {
        File signaturePlaceholderFile = new File(SIGNATURE_PLACEHOLDER_PATH);
        File signatureCallbackSignedFile = new File(SIGNATURE_CALLBACK_SIGNED_PATH);
        File signatureSignedFile = new File(SIGNATURE_SIGNED_PATH);
        try (
            PDDocument signaturePlaceholderDocument = Loader.loadPDF(signaturePlaceholderFile);
            PDDocument signatureCallbackSignedDocument = Loader.loadPDF(signatureCallbackSignedFile);
            PDDocument signatureSignedDocument = Loader.loadPDF(signatureSignedFile)
        ) {
            List<PDSignature> signaturePlaceholder = signaturePlaceholderDocument.getSignatureDictionaries();
            List<PDSignature> signatureCallbackSigned = signatureCallbackSignedDocument.getSignatureDictionaries();
            List<PDSignature> signatureSigned = signatureSignedDocument.getSignatureDictionaries();

            byte[] signaturePlaceholderData = Files.readAllBytes(signaturePlaceholderFile.toPath());
            byte[] signatureCallbackSignedData = Files.readAllBytes(signatureCallbackSignedFile.toPath());
            byte[] signatureSignedData = Files.readAllBytes(signatureSignedFile.toPath());

            String signaturePlaceholderContent = new String(signaturePlaceholderData, StandardCharsets.ISO_8859_1);
            String signatureCallbackSignedContent = new String(signatureCallbackSignedData, StandardCharsets.ISO_8859_1);
            String signatureSignedContent = new String(signatureSignedData, StandardCharsets.ISO_8859_1);

            System.out.println(signaturePlaceholderContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
