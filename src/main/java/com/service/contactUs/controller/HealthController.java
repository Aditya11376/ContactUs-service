package com.service.contactUs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class HealthController {

    @GetMapping(path = {"/", "/health", "/api/health", "/api/contactus/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "contact-us-service",
                "port", 8083,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
