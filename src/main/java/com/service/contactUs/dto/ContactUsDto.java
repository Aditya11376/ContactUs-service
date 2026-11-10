package com.service.contactUs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactUsDto(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Message cannot be blank")
        @Size(min=10, max=1000, message="Message must be between 10 to 1000 characters")
        String message,

        @NotBlank(message = "App name is required")
        String appName
) {
}
