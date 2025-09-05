package com.example.fbPdf.controller;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.event.StringPublisherService;
import com.example.fbPdf.service.DocumentService;
import com.example.fbPdf.service.PdfStyleService;
import com.example.fbPdf.service.S3Service;
import com.example.fbPdf.service.SigningService;
import com.example.fbPdf.service.signer.CloudSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;

@Slf4j
@RestController
@RequestMapping("/pdf")
public class PdfController {

    private final PdfStyleService pdfStyleService;
    private final S3Service s3Service;
    private final SigningService signingService;
    private final StringPublisherService stringPublisherService;
    private final DocumentService documentService;
    private final CloudSigner cloudSigner;

    public PdfController(PdfStyleService pdfStyleService,
                         S3Service s3Service,
                         SigningService signingService,
                         StringPublisherService stringPublisherService,
                         DocumentService documentService, CloudSigner cloudSigner) {
        this.pdfStyleService = pdfStyleService;
        this.s3Service = s3Service;
        this.signingService = signingService;
        this.stringPublisherService = stringPublisherService;
        this.documentService = documentService;
        this.cloudSigner = cloudSigner;
    }

    @PostMapping("/sign")
    public ResponseEntity<byte[]> signPdf(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam SigningType signingType
    ) throws Exception {

        File inputFile = new File("signature-test-file.pdf");
        byte[] signedPdf = signingService.signPdf(inputFile, signingType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signed.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(signedPdf);
    }

    @PostMapping("/callback-signing-request")
    public ResponseEntity<String> signingRequest(@RequestParam(required = false) String fileName) throws Exception {
        signingService.preparePdfForExternalSigning("signature-test-file.pdf");
        return ResponseEntity.ok("signing request ok");
    }

    @PostMapping("/callback")
    public ResponseEntity<String> callback() throws Exception {
        // Tạo chữ ký CMS giả lập (mock)
        byte[] contentToBeSigned;
        try (FileInputStream fis = new FileInputStream("contentToBeSigned.bin")) {
            contentToBeSigned = fis.readAllBytes();
        }
        byte[] cmsSignature = cloudSigner.signByExternalProvider(contentToBeSigned);

        signingService.callbackAsyncSignature(cmsSignature);
        return ResponseEntity.ok("callback ok");
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadFile(file));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename) throws Exception {
        byte[] content = s3Service.downloadFile(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @GetMapping("/create")
    public ResponseEntity<byte[]> createPdf() throws Exception {
        byte[] pdf = pdfStyleService.createStyledPdf();
        stringPublisherService.sendMessage("test message");
        System.out.println("Send msg | Thread: " + Thread.currentThread().getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=demo.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
