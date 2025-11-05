package com.campus.EventInClubs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @GetMapping("/email")
    public String testEmail(@RequestParam String toEmail) {
        try {
            log.info("Testing email to: {}", toEmail);
            log.info("Using from email: {}", fromEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Test Email from EventInClubs");
            message.setText("This is a test email. If you receive this, email configuration is working correctly!\n\nFrom: " + fromEmail);
            
            mailSender.send(message);
            
            log.info("Test email sent successfully to: {}", toEmail);
            return "Email sent successfully to " + toEmail + " from " + fromEmail;
            
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return "Failed to send email: " + e.getMessage();
        }
    }
}
