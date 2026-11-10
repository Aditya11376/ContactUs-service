package com.service.contactUs.service.impl;

import com.service.contactUs.dto.ContactUsDto;
import com.service.contactUs.entity.ContactUsMessage;
import com.service.contactUs.enums.RequestTypeEnum;
import com.service.contactUs.exception.SpamDetectedException;
import com.service.contactUs.repository.IContactUsRepository;
import com.service.contactUs.service.IContactUsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactUsServiceImpl implements IContactUsService {

    private final IContactUsRepository iContactUsRepository;
    private final RestClient geminiRestClient;

    @Override
    public void submitContactUsMessage(ContactUsDto contactUsDto) {
        boolean isSpam = false;
        RequestTypeEnum requestTypeEnum = RequestTypeEnum.GENERAL_INQUIRY;

        try {
            // Construct prompt for Gemini AI backend
            String prompt = String.format(
                    "Analyze the following message and output two fields separated by a pipe character (|): " +
                            "1) Is it spam? (true or false) " +
                            "2) Category (Choose strictly from: SUPPORT, FEEDBACK, GENERAL_INQUIRY, BUG_REPORT. " +
                            "Use FEEDBACK if the user expresses praise, compliments, or reviews the app). " +
                            "Message: \"%s\"",
                    contactUsDto.message()
            );

            Map<String, String> requestPayload = Map.of("message", prompt);

            Map<?, ?> response = geminiRestClient.post()
                    .uri("/gemini/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(Map.class);

            String aiResponse = response != null ? (String) response.get("reply") : null;

            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleanResponse = aiResponse.trim().replaceAll("```", "").trim();
                String upperResponse = cleanResponse.toUpperCase();

                String[] parts = upperResponse.split("\\|", 2);
                String spamPart = parts[0].trim();
                isSpam = spamPart.contains("TRUE") && !spamPart.contains("FALSE");

                String categoryPart = parts.length > 1 ? parts[1].trim() : upperResponse;
                if (categoryPart.contains("BUG_REPORT") || categoryPart.contains("BUG")) {
                    requestTypeEnum = RequestTypeEnum.BUG_REPORT;
                } else if (categoryPart.contains("FEEDBACK") || categoryPart.contains("PRAISE") || categoryPart.contains("COMPLIMENT") || categoryPart.contains("REVIEW")) {
                    requestTypeEnum = RequestTypeEnum.FEEDBACK;
                } else if (categoryPart.contains("SUPPORT") || categoryPart.contains("HELP")) {
                    requestTypeEnum = RequestTypeEnum.SUPPORT;
                } else if (categoryPart.contains("GENERAL_INQUIRY") || categoryPart.contains("INQUIRY")) {
                    requestTypeEnum = RequestTypeEnum.GENERAL_INQUIRY;
                } else {
                    requestTypeEnum = classifyMessageLocally(contactUsDto.message());
                }
            } else {
                requestTypeEnum = classifyMessageLocally(contactUsDto.message());
            }
        } catch (Exception e) {
            requestTypeEnum = classifyMessageLocally(contactUsDto.message());
        }

        if (isSpam) {
            throw new SpamDetectedException("Message flagged as spam by AI evaluation.");
        }

        ContactUsMessage contactUsMessage = new ContactUsMessage();
        contactUsMessage.setName(contactUsDto.name());
        contactUsMessage.setEmail(contactUsDto.email());
        contactUsMessage.setAppName(contactUsDto.appName());
        contactUsMessage.setMessage(contactUsDto.message());
        contactUsMessage.setRequestType(requestTypeEnum);

        iContactUsRepository.save(contactUsMessage);
    }

    private RequestTypeEnum classifyMessageLocally(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("bug") || lower.contains("error") || lower.contains("issue") || lower.contains("fail")) {
            return RequestTypeEnum.BUG_REPORT;
        } else if (lower.contains("feedback") || lower.contains("great") || lower.contains("love") || lower.contains("good") || lower.contains("nice")) {
            return RequestTypeEnum.FEEDBACK;
        } else if (lower.contains("help") || lower.contains("support") || lower.contains("assist")) {
            return RequestTypeEnum.SUPPORT;
        } else {
            return RequestTypeEnum.GENERAL_INQUIRY;
        }
    }
}
