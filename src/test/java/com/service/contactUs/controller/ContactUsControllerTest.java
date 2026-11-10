package com.service.contactUs.controller;

import com.service.contactUs.dto.ContactUsDto;
import com.service.contactUs.exception.ContactUsExceptionHandler;
import com.service.contactUs.exception.SpamDetectedException;
import com.service.contactUs.service.IContactUsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContactUsControllerTest {

    private static final String SECRET = "CS2bWg53GM6tpP0CrCTcg85RbCYwrXbOH3yVLUuJNCtk11qQkdukQGPSY4LC+J44zE26Qec7fIqLZElJ/pNP1g==";

    @Mock
    private IContactUsService iContactUsService;

    @InjectMocks
    private ContactUsController contactUsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactUsController, "expectedSecret", SECRET);
        mockMvc = MockMvcBuilders.standaloneSetup(contactUsController)
                .setControllerAdvice(new ContactUsExceptionHandler())
                .build();
    }

    @Test
    void submitContactUsMessage_withoutSecretHeader_returnsUnauthorized() throws Exception {
        String json = """
                {
                    "name": "John",
                    "email": "john@example.com",
                    "message": "This is a valid message.",
                    "appName": "VoltFind"
                }
                """;

        mockMvc.perform(post("/api/contact-us")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());

        verify(iContactUsService, never()).submitContactUsMessage(any());
    }

    @Test
    void submitContactUsMessage_withInvalidSecretHeader_returnsUnauthorized() throws Exception {
        String json = """
                {
                    "name": "John",
                    "email": "john@example.com",
                    "message": "This is a valid message.",
                    "appName": "VoltFind"
                }
                """;

        mockMvc.perform(post("/api/contact-us")
                        .header("X-Internal-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());

        verify(iContactUsService, never()).submitContactUsMessage(any());
    }

    @Test
    void submitContactUsMessage_withInvalidDto_returnsBadRequest() throws Exception {
        // Message too short (< 10 chars) and invalid email
        String json = """
                {
                    "name": "",
                    "email": "not-an-email",
                    "message": "short",
                    "appName": ""
                }
                """;

        mockMvc.perform(post("/api/contact-us")
                        .header("X-Internal-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.appName").exists());

        verify(iContactUsService, never()).submitContactUsMessage(any());
    }

    @Test
    void submitContactUsMessage_whenSpamDetected_returnsBadRequest() throws Exception {
        String json = """
                {
                    "name": "Spammer",
                    "email": "spammer@example.com",
                    "message": "Buy cheap crypto right now on our website!",
                    "appName": "VoltFind"
                }
                """;
        doThrow(new SpamDetectedException("Message flagged as spam by AI evaluation."))
                .when(iContactUsService).submitContactUsMessage(any(ContactUsDto.class));

        mockMvc.perform(post("/api/contact-us")
                        .header("X-Internal-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Message flagged as spam by AI evaluation."));
    }

    @Test
    void submitContactUsMessage_withValidRequest_returnsCreated() throws Exception {
        String json = """
                {
                    "name": "Jane Doe",
                    "email": "jane@example.com",
                    "message": "Hello, I have a question about charging ports.",
                    "appName": "VoltFind"
                }
                """;

        mockMvc.perform(post("/api/contact-us")
                        .header("X-Internal-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Message submitted successfully!"));

        verify(iContactUsService, times(1)).submitContactUsMessage(any(ContactUsDto.class));
    }
}
