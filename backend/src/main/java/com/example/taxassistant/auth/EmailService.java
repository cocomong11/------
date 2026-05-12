package com.example.taxassistant.auth;

public interface EmailService {

    void sendVerificationCode(String email, String code);

    void sendPasswordResetLink(String email, String resetToken);
}
