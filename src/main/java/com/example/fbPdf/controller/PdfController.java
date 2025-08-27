package com.example.fbPdf.controller;

import com.example.fbPdf.enums.SigningProviderType;
import com.example.fbPdf.event.StringPublisherService;
import com.example.fbPdf.service.PdfService;
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

    private final PdfService pdfService;
    private final S3Service s3Service;
    private final SigningService signingService;
    private final StringPublisherService stringPublisherService;

    public PdfController(PdfService pdfService, S3Service s3Service, SigningService signingService, StringPublisherService stringPublisherService) {
        this.pdfService = pdfService;
        this.s3Service = s3Service;
        this.signingService = signingService;
        this.stringPublisherService = stringPublisherService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String url = s3Service.uploadFile(file);
        return ResponseEntity.ok(url);
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
        byte[] pdf = pdfService.createStyledPdf();
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
            @RequestParam SigningProviderType signingType
    ) throws Exception {

        InputStream inputStream = file.getInputStream();

        byte[] signedPdf = signingService.signPdf(inputStream, signingType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signed.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(signedPdf);
    }
}
