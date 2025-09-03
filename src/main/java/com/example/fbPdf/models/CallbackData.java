package com.example.fbPdf.models;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CallbackData {
    private String signature;
    private String algorithm;
    private String digest;
}