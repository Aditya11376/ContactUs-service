package com.service.contactUs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Lazy(false)
@Configuration
public class AppConfig {

    @Value("${gemini.service.url")
    private String geminiServiceUrl;

    @Bean
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));

        return RestClient.builder()
                .baseUrl(geminiServiceUrl)
                .requestFactory(factory)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpGeminiService() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending background warm-up ping to Gemini service...");
                RestClient.builder()
                        .baseUrl(geminiServiceUrl)
                        .build()
                        .get()
                        .uri("/health")
                        .retrieve()
                        .toBodilessEntity();
                log.info("Gemini service warm-up ping completed.");
            } catch (Exception e) {
                log.debug("Gemini service warm-up ping skipped: {}", e.getMessage());
            }
        });
    }
}
