package com.example.fbPdf.controller;

import com.example.fbPdf.event.StringPublisherService;
import com.example.fbPdf.service.PdfService;
import com.example.fbPdf.service.SigningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/pdf")
public class PdfController {

    private final PdfService pdfService;
    private final SigningService signingService;
    private final StringPublisherService stringPublisherService;

    public PdfController(PdfService pdfService, SigningService signingService, StringPublisherService stringPublisherService) {
        this.pdfService = pdfService;
        this.signingService = signingService;
        this.stringPublisherService = stringPublisherService;
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

//    @GetMapping("/sign")
//    public ResponseEntity<byte[]> signPdf(Miltipart) throws Exception {
//        byte[] pdf = pdfService.createStyledPdf();
//        log.info("pdf byte: " + Arrays.toString(pdf));
//        byte[] signed = signingService.signPdf(pdf);
//        log.info("signed byte: " + Arrays.toString(signed));
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=signed.pdf")
//                .contentType(MediaType.APPLICATION_PDF)
//                .body(signed);
//    }

    @PostMapping("/sign")
    public ResponseEntity<byte[]> signPdf(@RequestParam("file") MultipartFile file) throws Exception {
        // Load PDF từ MultipartFile
        InputStream inputStream = file.getInputStream();

        // Gọi hàm ký PDF
        byte[] signedPdf = signingService.signPdfIncremental(inputStream);

        // Trả về file PDF đã ký
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signed.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(signedPdf);
    }
}
