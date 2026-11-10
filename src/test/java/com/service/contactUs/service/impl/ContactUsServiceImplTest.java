package com.service.contactUs.service.impl;

import com.service.contactUs.dto.ContactUsDto;
import com.service.contactUs.entity.ContactUsMessage;
import com.service.contactUs.enums.RequestTypeEnum;
import com.service.contactUs.exception.SpamDetectedException;
import com.service.contactUs.repository.IContactUsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class ContactUsServiceImplTest {

    @Mock
    private IContactUsRepository iContactUsRepository;

    private MockRestServiceServer mockServer;
    private ContactUsServiceImpl contactUsService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8092");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        contactUsService = new ContactUsServiceImpl(iContactUsRepository, builder.build());
    }

    @Test
    void submitContactUsMessage_withValidAiResponse_savesMessageWithCategory() {
        mockServer.expect(requestTo("http://localhost:8092/gemini/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"reply\":\"false | BUG_REPORT\"}", MediaType.APPLICATION_JSON));

        ContactUsDto dto = new ContactUsDto(
                "John Doe",
                "john@example.com",
                "I found a bug in the charging station list map view",
                "VoltFind"
        );

        contactUsService.submitContactUsMessage(dto);

        mockServer.verify();
        ArgumentCaptor<ContactUsMessage> captor = ArgumentCaptor.forClass(ContactUsMessage.class);
        verify(iContactUsRepository).save(captor.capture());

        ContactUsMessage saved = captor.getValue();
        assertEquals("John Doe", saved.getName());
        assertEquals("john@example.com", saved.getEmail());
        assertEquals("VoltFind", saved.getAppName());
        assertEquals("I found a bug in the charging station list map view", saved.getMessage());
        assertEquals(RequestTypeEnum.BUG_REPORT, saved.getRequestType());
    }

    @Test
    void submitContactUsMessage_whenAiDetectsSpam_throwsSpamDetectedException() {
        mockServer.expect(requestTo("http://localhost:8092/gemini/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"reply\":\"true | GENERAL_INQUIRY\"}", MediaType.APPLICATION_JSON));

        ContactUsDto dto = new ContactUsDto(
                "Spammer",
                "spam@example.com",
                "Buy cheap products at http://spam.example.com right now!",
                "VoltFind"
        );

        assertThrows(SpamDetectedException.class, () -> contactUsService.submitContactUsMessage(dto));
        verify(iContactUsRepository, never()).save(any());
        mockServer.verify();
    }

    @Test
    void submitContactUsMessage_whenAiServiceFails_fallsBackToLocalClassification() {
        mockServer.expect(requestTo("http://localhost:8092/gemini/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        ContactUsDto dto = new ContactUsDto(
                "Jane Doe",
                "jane@example.com",
                "I love this app, great service and very useful!",
                "VoltFind"
        );

        contactUsService.submitContactUsMessage(dto);

        mockServer.verify();
        ArgumentCaptor<ContactUsMessage> captor = ArgumentCaptor.forClass(ContactUsMessage.class);
        verify(iContactUsRepository).save(captor.capture());

        ContactUsMessage saved = captor.getValue();
        assertEquals(RequestTypeEnum.FEEDBACK, saved.getRequestType());
    }
}
