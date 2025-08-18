package com.example.fbPdf.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Calendar;

@Slf4j
@Service
public class SigningService {

    public byte[] signPdf(byte[] inputPdf) throws Exception {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(inputPdf))) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE); 
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Demo User");
            signature.setLocation("VN");
            signature.setReason("Testing Digital Signature");
            signature.setSignDate(Calendar.getInstance());

            log.info("signature " + signature);
            document.addSignature(signature);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] signPdfIncremental(InputStream inputPdf) throws Exception {
        try (PDDocument document = PDDocument.load(inputPdf)) {

            // Tạo trường chữ ký
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE); // Adobe PPKLite
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED); // Detached signature
            signature.setName("Demo User");
            signature.setLocation("Hanoi, Vietnam");
            signature.setReason("Digital signing demo");
            signature.setSignDate(Calendar.getInstance());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Thêm signature vào document
            document.addSignature(signature, new SignatureInterface() {
                @Override
                public byte[] sign(InputStream content) throws IOException {
                    // TODO: Thay bằng mã ký thực (private key + cert chain)
                    // Hiện tại chỉ tạo chữ ký giả cho demo
                    return "DUMMY_SIGNATURE".getBytes(StandardCharsets.UTF_8);
                }
            });

            // Ghi incremental (append mode)
            document.saveIncremental(outputStream);
            return outputStream.toByteArray();
        }
    }
}
