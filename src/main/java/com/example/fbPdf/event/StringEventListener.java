package com.example.fbPdf.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StringEventListener {

    @EventListener
    public void handleStringEvent(String event) throws InterruptedException {
        Thread.sleep(3000);
        System.out.println("Received String event: " + event 
                           + " | Thread: " + Thread.currentThread().getName());
//        throw new RuntimeException("Listener failed!");
    }
}
