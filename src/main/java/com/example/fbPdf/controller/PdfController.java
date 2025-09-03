package com.example.fbPdf.controller;

import com.example.fbPdf.enums.SigningType;
import com.example.fbPdf.event.StringPublisherService;
import com.example.fbPdf.service.DocumentService;
import com.example.fbPdf.service.PdfStyleService;
import com.example.fbPdf.service.S3Service;
import com.example.fbPdf.service.SigningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/pdf")
public class PdfController {

    private final PdfStyleService pdfStyleService;
    private final S3Service s3Service;
    private final SigningService signingService;
    private final StringPublisherService stringPublisherService;
    private final DocumentService documentService;

    public PdfController(PdfStyleService pdfStyleService,
                         S3Service s3Service,
                         SigningService signingService,
                         StringPublisherService stringPublisherService,
                         DocumentService documentService) {
        this.pdfStyleService = pdfStyleService;
        this.s3Service = s3Service;
        this.signingService = signingService;
        this.stringPublisherService = stringPublisherService;
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadFile(file));
    }

    @PostMapping("/signing-request")
    public ResponseEntity<String> signingRequest(@RequestParam("fileName") String fileName) throws Exception {
        return ResponseEntity.ok(signingService.preparePdfForExternalSigning(fileName));
    }

    @PostMapping("/callback")
    public ResponseEntity<String> callback() throws Exception {
        byte[] cmsSignature = new byte[0];
        signingService.callbackMoit(cmsSignature);
        return ResponseEntity.ok("callback ok");
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

    @PostMapping("/sign")
    public ResponseEntity<byte[]> signPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam SigningType signingType
    ) throws Exception {

        InputStream inputStream = file.getInputStream();

        byte[] signedPdf = signingService.signPdf(inputStream, signingType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signed.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(signedPdf);
    }
}
