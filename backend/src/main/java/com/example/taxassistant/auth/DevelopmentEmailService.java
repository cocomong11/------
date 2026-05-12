package com.example.taxassistant.auth;

import org.springframework.stereotype.Service;

@Service
public class DevelopmentEmailService implements EmailService {

    @Override
    public void sendVerificationCode(String email, String code) {
        // Development mode stores the code in the database; replace this service with SMTP later.
    }

    @Override
    public void sendPasswordResetLink(String email, String resetToken) {
        // Development mode stores the token in the database; replace this service with SMTP later.
    }
}
