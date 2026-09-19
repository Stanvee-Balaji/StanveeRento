

package com.example.demo.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.context.annotation.Bean;

import org.springframework.web.client.RestTemplate;


@Configuration
public class CorsConfig implements WebMvcConfigurer {
	
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")        // ✅ covers ALL routes
                .allowedOrigins(
                        "https://shop.stanvee.com",
                        "http://127.0.0.1:5500",
                        "http://127.0.0.1:5501",
                        "http://localhost:5500",
                        "http://localhost:8080",
                        "http://127.0.0.1:5501/",
                        "http://51.21.152.29:8080",
                        "http://51.21.152.29",
                        "https://stanveeshop.com",
                        "https://www.stanveeshop.com",
                        "https://paypandastore.com",
                        "https://www.paypandastore.com"
                 )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);    // ✅ needed for auth/cookies
    }
}