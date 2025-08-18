package com.example.fbPdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FbPdfApplication {

	public static void main(String[] args) {
		SpringApplication.run(FbPdfApplication.class, args);
	}

}
