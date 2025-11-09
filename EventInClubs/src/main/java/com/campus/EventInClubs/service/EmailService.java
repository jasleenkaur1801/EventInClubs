package com.campus.EventInClubs.service;

import com.campus.EventInClubs.domain.model.Event;
import com.campus.EventInClubs.domain.model.EventRegistration;
import com.campus.EventInClubs.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * Send registration confirmation email to student
     */
    public void sendRegistrationConfirmation(EventRegistration registration, String clubAdminEmail) {
        try {
            log.info("Starting email send process for registration ID: {}", registration.getId());
            log.info("From email configured as: {}", fromEmail);
            log.info("Recipient: {}, Reply-to: {}", registration.getUser().getEmail(), clubAdminEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Event event = registration.getEvent();
            User student = registration.getUser();
            
            // Use authenticated email as sender, set club admin as reply-to
            helper.setFrom(fromEmail);
            helper.setReplyTo(clubAdminEmail);
            helper.setTo(student.getEmail());
            helper.setSubject("Registration Confirmed: " + event.getTitle());
            
            log.info("Building email body for event: {}", event.getTitle());
            String emailBody = buildRegistrationEmail(registration, event, student);
            helper.setText(emailBody, true); // true = HTML email
            
            log.info("Attempting to send email via SMTP...");
            mailSender.send(message);
            log.info("✅ Registration confirmation email successfully sent to {} for event {}", student.getEmail(), event.getTitle());
            
        } catch (MessagingException e) {
            log.error("❌ MessagingException while sending email: {}", e.getMessage(), e);
            log.error("Email details - From: {}, To: {}, Event: {}", 
                fromEmail, registration.getUser().getEmail(), registration.getEvent().getTitle());
            // Don't throw exception - email failure shouldn't break registration
        } catch (Exception e) {
            log.error("❌ Unexpected error sending registration confirmation email: {}", e.getMessage(), e);
            log.error("Stack trace:", e);
        }
    }
    
    /**
     * Send event reminder email on the day of the event
     */
    public void sendEventReminder(EventRegistration registration, String clubAdminEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Event event = registration.getEvent();
            User student = registration.getUser();
            
            // Use authenticated email as sender, set club admin as reply-to
            helper.setFrom(fromEmail);
            helper.setReplyTo(clubAdminEmail);
            helper.setTo(student.getEmail());
            helper.setSubject("Reminder: " + event.getTitle() + " Today!");
            
            String emailBody = buildReminderEmail(registration, event, student);
            helper.setText(emailBody, true);
            
            mailSender.send(message);
            log.info("Event reminder email sent to {} for event {}", student.getEmail(), event.getTitle());
            
        } catch (MessagingException e) {
            log.error("Failed to send event reminder email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending event reminder email", e);
        }
    }
    
    /**
     * Build HTML email for registration confirmation
     */
    private String buildRegistrationEmail(EventRegistration registration, Event event, User student) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .event-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .detail-row { display: flex; margin: 10px 0; }
                    .detail-label { font-weight: bold; width: 150px; color: #667eea; }
                    .detail-value { flex: 1; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .btn { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Registration Confirmed!</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>You have successfully registered for the following event:</p>
                        
                        <div class="event-details">
                            <h2 style="color: #667eea; margin-top: 0;">%s</h2>
                            <div class="detail-row">
                                <span class="detail-label">📅 Date & Time:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">📍 Location:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">🎪 Organized by:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">💰 Fee:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">🎫 Registration ID:</span>
                                <span class="detail-value">#%d</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">✅ Status:</span>
                                <span class="detail-value" style="color: green; font-weight: bold;">%s</span>
                            </div>
                        </div>
                        
                        <p><strong>Important:</strong></p>
                        <ul>
                            <li>Please arrive 10 minutes before the event starts</li>
                            <li>Bring a valid student ID for verification</li>
                            <li>You will receive a reminder email on the day of the event</li>
                        </ul>
                        
                        <p>If you have any questions, please contact the event organizer.</p>
                        
                        <p>See you at the event!</p>
                        
                        <div class="footer">
                            <p>This is an automated email from Event Idea Marketplace.<br>
                            Please do not reply to this email.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            student.getName(),
            event.getTitle(),
            event.getStartDate().format(formatter),
            event.getLocation() != null ? event.getLocation() : "TBA",
            event.getClub().getName(),
            event.getRegistrationFee() == null || event.getRegistrationFee() == 0 ? "Free" : "₹" + event.getRegistrationFee(),
            registration.getId(),
            registration.getStatus().toString()
        );
    }
    
    /**
     * Build HTML email for event reminder
     */
    private String buildReminderEmail(EventRegistration registration, Event event, User student) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .reminder-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 20px; margin: 20px 0; border-radius: 5px; }
                    .event-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .detail-row { display: flex; margin: 10px 0; }
                    .detail-label { font-weight: bold; width: 150px; color: #f5576c; }
                    .detail-value { flex: 1; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⏰ Event Reminder!</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <div class="reminder-box">
                            <h3 style="margin-top: 0; color: #856404;">🎯 Don't Forget!</h3>
                            <p style="margin: 0; font-size: 16px;"><strong>Your registered event is TODAY!</strong></p>
                        </div>
                        
                        <div class="event-details">
                            <h2 style="color: #f5576c; margin-top: 0;">%s</h2>
                            <div class="detail-row">
                                <span class="detail-label">⏰ Time:</span>
                                <span class="detail-value" style="font-size: 18px; font-weight: bold;">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">📍 Location:</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">🎪 Organized by:</span>
                                <span class="detail-value">%s</span>
                            </div>
                        </div>
                        
                        <p><strong>Checklist before you leave:</strong></p>
                        <ul>
                            <li>✅ Student ID Card</li>
                            <li>✅ Registration ID: #%d</li>
                            <li>✅ Arrive 10 minutes early</li>
                            <li>✅ Any required materials or prerequisites</li>
                        </ul>
                        
                        <p style="font-size: 16px; color: #f5576c;"><strong>We're excited to see you there!</strong></p>
                        
                        <div class="footer">
                            <p>This is an automated reminder from Event Idea Marketplace.<br>
                            Please do not reply to this email.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            student.getName(),
            event.getTitle(),
            event.getStartDate().format(timeFormatter),
            event.getLocation() != null ? event.getLocation() : "TBA",
            event.getClub().getName(),
            registration.getId()
        );
    }
}
