# Email Notification Setup Guide

This guide explains how to configure email notifications for event registrations and reminders in the EventInClubs application.

## Features Implemented

### 1. **Registration Confirmation Email**
- ✅ Sent immediately when a student registers for an event
- ✅ Sent from the club admin's email address
- ✅ Contains event details, registration ID, and important instructions
- ✅ Beautiful HTML email template

### 2. **Event Reminder Email**
- ✅ Sent automatically on the day of the event at 8:00 AM
- ✅ Sent from the club admin's email address
- ✅ Contains event time, location, and checklist
- ✅ Scheduled task runs daily to check for events

## Setup Instructions

### Step 1: Configure Gmail Account

1. **Enable 2-Factor Authentication** on your Gmail account:
   - Go to https://myaccount.google.com/security
   - Enable 2-Step Verification

2. **Generate App Password**:
   - Go to https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)"
   - Name it "EventInClubs"
   - Copy the 16-character password

### Step 2: Update application.properties

Open `/EventInClubs/src/main/resources/application.properties` and update:

```properties
# Email Configuration
spring.mail.username=your-club-email@gmail.com
spring.mail.password=your-16-char-app-password
```

**Replace:**
- `your-club-email@gmail.com` with the club admin's Gmail address
- `your-16-char-app-password` with the App Password from Step 1

### Step 3: Rebuild the Project

```bash
cd EventInClubs
mvn clean install
```

### Step 4: Restart the Application

```bash
mvn spring-boot:run
```

## Email Templates

### Registration Confirmation Email
Sent when a student registers for an event:
- **Subject:** "Registration Confirmed: [Event Name]"
- **Content:**
  - Event details (date, time, location)
  - Registration ID
  - Club information
  - Important reminders
  - Arrival instructions

### Event Reminder Email
Sent on the day of the event at 8:00 AM:
- **Subject:** "Reminder: [Event Name] Today!"
- **Content:**
  - Event time and location
  - Pre-event checklist
  - Registration ID
  - Arrival instructions

## How It Works

### Registration Flow:
1. Student registers for an event via the frontend
2. Backend creates registration in database
3. In-app notification sent to student and club admin
4. **Email confirmation sent to student** from club admin's email
5. Registration complete

### Reminder Flow:
1. Scheduled task runs every day at 8:00 AM
2. System checks for events happening today
3. For each event found, get all registered students
4. **Send reminder email to each registered student** from club admin's email

## Scheduled Task Details

**Cron Expression:** `0 0 8 * * *`
- Runs every day at 8:00 AM Indian Standard Time
- Automatically checks for events starting that day
- Sends reminders to all registered students

## Customization

### Change Reminder Time
Edit `/EventInClubs/src/main/java/com/campus/EventInClubs/service/EventReminderService.java`:

```java
@Scheduled(cron = "0 0 8 * * *") // Change 8 to your preferred hour (24-hour format)
```

Examples:
- `0 0 7 * * *` - 7:00 AM
- `0 30 9 * * *` - 9:30 AM
- `0 0 20 * * *` - 8:00 PM

### Customize Email Templates
Edit `/EventInClubs/src/main/java/com/campus/EventInClubs/service/EmailService.java`:
- Modify `buildRegistrationEmail()` for confirmation emails
- Modify `buildReminderEmail()` for reminder emails

## Testing

### Test Registration Email:
1. Register for an event as a student
2. Check the registered email inbox
3. You should receive a confirmation email immediately

### Test Reminder Email:
1. Create an event with today's date
2. Register for the event
3. Wait for 8:00 AM OR manually trigger:
   - Call the EventReminderService manually from code
   - Or change the cron schedule to run sooner

## Troubleshooting

### Email Not Sending

**Check 1: App Password**
- Ensure you're using an App Password, not your regular Gmail password
- App Password must be 16 characters without spaces

**Check 2: Gmail Security**
- Verify 2-Factor Authentication is enabled
- Check if Gmail blocked the login attempt

**Check 3: Application Logs**
Look for errors in the console:
```bash
Failed to send registration confirmation email
Failed to send event reminder email
```

**Check 4: Email Configuration**
Verify `application.properties` has correct:
- `spring.mail.username`
- `spring.mail.password`
- `spring.mail.host=smtp.gmail.com`
- `spring.mail.port=587`

### Emails Going to Spam

1. Add the sender email to contacts
2. Mark first email as "Not Spam"
3. Create filter to always trust this sender

### Schedule Not Running

1. Verify `@EnableScheduling` is present in main application class
2. Check application logs for "Starting daily event reminder job..."
3. Verify system timezone matches cron schedule

## Alternative Email Providers

### Using Outlook/Hotmail:
```properties
spring.mail.host=smtp.office365.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
```

### Using Custom SMTP Server:
```properties
spring.mail.host=your-smtp-server.com
spring.mail.port=587
spring.mail.username=your-email
spring.mail.password=your-password
```

## Security Notes

⚠️ **Important:**
- Never commit `application.properties` with real credentials to Git
- Use environment variables in production
- Keep App Passwords secure
- Regularly rotate App Passwords

## Support

If you encounter issues:
1. Check application logs
2. Verify email configuration
3. Test with a different email address
4. Check Gmail security settings

---

**Email Notification System**
- Author: EventInClubs Team
- Last Updated: November 3, 2024
- Version: 1.0
