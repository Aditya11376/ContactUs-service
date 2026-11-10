package com.service.contactUs.service;

import com.service.contactUs.dto.ContactUsDto;
import jakarta.validation.Valid;

public interface IContactUsService {
    void submitContactUsMessage(@Valid ContactUsDto contactUsDto);
}
