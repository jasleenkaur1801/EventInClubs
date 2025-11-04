package com.campus.EventInClubs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmailTestController {
    
    private final JavaMailSender mailSender;
    
    @GetMapping("/email")
    public String testEmail(@RequestParam String toEmail) {
        try {
            log.info("Testing email to: {}", toEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("eventinclubs@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Test Email from EventInClubs");
            message.setText("This is a test email. If you receive this, email configuration is working correctly!");
            
            mailSender.send(message);
            
            log.info("Test email sent successfully to: {}", toEmail);
            return "Email sent successfully to " + toEmail;
            
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return "Failed to send email: " + e.getMessage();
        }
    }
}
