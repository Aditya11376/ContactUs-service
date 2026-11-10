package com.service.contactUs.exception;

public class SpamDetectedException extends RuntimeException {
    public SpamDetectedException(String message) {
        super(message);
    }
}
