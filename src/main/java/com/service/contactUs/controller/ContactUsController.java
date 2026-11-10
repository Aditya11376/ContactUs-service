package com.service.contactUs.controller;

import com.service.contactUs.dto.ContactUsDto;
import com.service.contactUs.service.IContactUsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContactUsController {

    private final IContactUsService iContactUsService;

    @Value("${app.internal.secret}")
    private String expectedSecret;

    @PostMapping(path = {"/contact-us"})
    public ResponseEntity<Map<String, String>> submitContactUsMessage(@Valid @RequestBody ContactUsDto contactUsDto,
                                                                     @RequestHeader(value = "X-Internal-Secret", required = false) String secretHeader) {
        boolean isAuthorized = expectedSecret != null &&
                secretHeader != null &&
                MessageDigest.isEqual(expectedSecret.getBytes(StandardCharsets.UTF_8), secretHeader.getBytes(StandardCharsets.UTF_8));

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        iContactUsService.submitContactUsMessage(contactUsDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Message submitted successfully!"));
    }
}
