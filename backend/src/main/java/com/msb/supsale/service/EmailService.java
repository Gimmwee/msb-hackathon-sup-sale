package com.msb.supsale.service;

public interface EmailService {
    void send(String to, String subject, String body, boolean isMock);
}
