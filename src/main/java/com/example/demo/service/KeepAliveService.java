package com.example.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAliveService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 1 * 40 * 1000)
    
    public void keepAlive() {

        try {
            String url = "https://stanveerento.onrender.com/api/health";

            String response = restTemplate.getForObject(
                    url,
                    String.class
            );

            System.out.println(
                    "[KEEP-ALIVE] Health API called successfully: " + response
            );

        } catch (Exception e) {

            System.out.println(
                    "[KEEP-ALIVE] Health API call failed: " + e.getMessage()
            );
        }
    }
    
    
}