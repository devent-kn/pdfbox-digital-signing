//package com.example.fbPdf.factory;
//
//import lombok.Getter;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.pdfbox.cos.COSName;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
//import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
//import org.bouncycastle.util.encoders.Base64;
//import org.bouncycastle.util.encoders.Hex;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.GeneralSecurityException;
//import java.security.cert.Certificate;
//import java.util.Calendar;
//
//@Slf4j
//public class SignatureContainerFactory {
//
//    SignatureContainerFactory() {
//        throw new IllegalStateException("Utility class");
//    }
//
//    public static EsignBlankSignatureContainer createBlankContainer(COSName filter, COSName subFilter) {
//        return new MOITExternalBlankSignatureContainer(filter, subFilter);
//        };
//    }
//
//    public static IExternalSignatureContainer createContainer(SignatureContainerType type,
//                                                              String base64Signature,
//                                                              Certificate[] certChain,
//                                                              ITSAClient tsaClient) {
//        return new MOITExternalSignatureContainer(base64Signature, certChain, tsaClient);
//    }
//
//    @Getter
//    public static class EsignBlankSignatureContainer  {
//
//        @Getter
//        protected String hash;
//
//        @Setter
//        protected Certificate[] chain;
//
//
//        public EsignBlankSignatureContainer(PdfName filter, PdfName subFilter) {
//            super(filter, subFilter);
//        }
//
//    }
//
//
//    @Slf4j
//    public static class ExternalSignatureContainer {
//
//        protected final String base64Signature;
//
//        protected final Certificate[] certChain;
//
//        protected ITSAClient tsaClient;
//
//        public ExternalSignatureContainer(String base64Signature,
//                                          Certificate[] certChain,
//                                          ITSAClient tsaClient) {
//            this.base64Signature = base64Signature;
//            this.certChain = certChain;
//            this.tsaClient = tsaClient;
//        }
//
//
//        @Override
//        public byte[] sign(InputStream data) throws GeneralSecurityException {
//            return new byte[0];
//        }
//
//    }
//
//
//    @Slf4j
//    public static class MOITExternalSignatureContainer extends ExternalSignatureContainer {
//
//        public MOITExternalSignatureContainer(String hexSignature,
//                                              Certificate[] certChain,
//                                              ITSAClient tsaClient) {
//            super(hexSignature, certChain, tsaClient);
//        }
//
//        @Override
//        public byte[] sign(InputStream data) throws GeneralSecurityException {
//            try {
//                BouncyCastleDigest digest = new BouncyCastleDigest();
//                PdfPKCS7 sgn = new PdfPKCS7(null, certChain, Constants.HASH_ALGORITHM, null, digest, false);
//                byte[] hash = DigestAlgorithms.digest(data, digest.getMessageDigest(Constants.HASH_ALGORITHM));
//
//                byte[] extSignature = Hex.decode(base64Signature);
//                sgn.setExternalDigest(extSignature, null, Constants.ENCRYPT_ALGORITHM);
//                return sgn.getEncodedPKCS7(hash, PdfSigner.CryptoStandard.CADES, tsaClient, null, null);
//            } catch (IOException e) {
//                log.info("Error when try to build hash", e);
//            }
//            return new byte[0];
//        }
//    }
//
//    static class MOITExternalBlankSignatureContainer {
//
//        private final COSName filter;
//        private final COSName subFilter;
//        @Getter
//        protected String hash;
//        @Setter
//        protected Certificate[] chain;
//
//        public MOITExternalBlankSignatureContainer(COSName filter, COSName subFilter) {
//            this.filter = filter;
//            this.subFilter = subFilter;
//        }
//
//        public byte[] sign(InputStream pdfInput) throws IOException {
//            try (PDDocument document = PDDocument.load(pdfInput)) {
//                PDSignature signature = new PDSignature();
//                signature.setFilter(filter);
//                signature.setSubFilter(subFilter);
//                signature.setName("Demo User");
//                signature.setLocation("VN");
//                signature.setReason("External signing");
//                signature.setSignDate(Calendar.getInstance());
//
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//                SignatureOptions options = new SignatureOptions();
//                options.setPreferredSignatureSize(8192 * 2);
//                document.addSignature(signature, options);
//        }
//    }
//
//
//}
