package com.example.fbPdf.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class StringPublisherService {
    private final ApplicationEventPublisher publisher;

    public StringPublisherService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void sendMessage(String msg) {
        System.out.println("Publishing event: " + msg + " | Thread: " + Thread.currentThread().getName());
        publisher.publishEvent(msg); // phát sự kiện String
    }
}
