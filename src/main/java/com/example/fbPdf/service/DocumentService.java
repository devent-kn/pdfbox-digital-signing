package com.example.fbPdf.service;

import ch.qos.logback.core.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentService {

    public String uploadFile(MultipartFile file) {
        try {
            String currentDir = System.getProperty("user.dir");
            Path path = Paths.get(currentDir, file.getOriginalFilename());
            Files.write(path, file.getBytes());
            return path.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
