# Setup Instructions for EventInClubs

## ⚠️ IMPORTANT: First Time Setup

After cloning this repository, you need to configure sensitive credentials that are NOT included in Git.

### Step 1: Create application.properties

1. Navigate to: `EventInClubs/src/main/resources/`
2. Copy `application.properties.template` to `application.properties`
   ```bash
   cd EventInClubs/src/main/resources/
   cp application.properties.template application.properties
   ```

### Step 2: Configure Database

Edit `application.properties` and update these values:

```properties
# Replace with your Neon/PostgreSQL database credentials
spring.datasource.url=jdbc:postgresql://YOUR_DATABASE_HOST/YOUR_DATABASE_NAME?sslmode=require&channel_binding=require&prepareThreshold=0
spring.datasource.username=YOUR_DATABASE_USERNAME
spring.datasource.password=YOUR_DATABASE_PASSWORD
```

### Step 3: Configure Email

Update email configuration in `application.properties`:

```properties
# Replace with your Gmail account
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_16_CHAR_APP_PASSWORD_NO_SPACES
```

**How to get Gmail App Password:**
1. Enable 2-Factor Authentication on your Gmail
2. Go to https://myaccount.google.com/apppasswords
3. Generate new app password
4. Copy the 16-character password WITHOUT spaces
5. Paste in application.properties

See `EMAIL_SETUP.md` for detailed email configuration guide.

### Step 4: Install Dependencies

**Backend:**
```bash
cd EventInClubs
mvn clean install
```

**Frontend:**
```bash
cd Frontend
npm install
```

### Step 5: Run the Application

**Backend (Terminal 1):**
```bash
cd EventInClubs
mvn spring-boot:run
```

**Frontend (Terminal 2):**
```bash
cd Frontend
npm run dev
```

## 🔐 Security Notes

- **NEVER commit `application.properties`** - it contains sensitive credentials
- The `.gitignore` file is configured to exclude this file
- Always use the template file for reference
- Keep your database and email credentials secure

## 📝 What's Excluded from Git

The following files/folders are NOT tracked in Git (see `.gitignore`):
- `application.properties` - Contains database and email credentials
- `node_modules/` - Frontend dependencies
- `target/` - Backend build files
- `.env` files - Environment variables
- `uploads/` - User uploaded files
- IDE configuration files

## 🚀 Quick Start Checklist

- [ ] Clone repository
- [ ] Create `application.properties` from template
- [ ] Configure database credentials
- [ ] Configure email credentials (Gmail app password)
- [ ] Run `mvn clean install` in EventInClubs folder
- [ ] Run `npm install` in Frontend folder
- [ ] Start backend server
- [ ] Start frontend dev server
- [ ] Access application at http://localhost:5173

## 📚 Additional Documentation

- `EMAIL_SETUP.md` - Email notification setup guide
- `EMAIL_TROUBLESHOOTING.md` - Email troubleshooting guide
- `README.md` - Project overview (if exists)

## 🆘 Need Help?

If you encounter issues:
1. Check that `application.properties` exists and has correct credentials
2. Verify database connection
3. Test email configuration using test endpoint
4. Check backend logs for error messages
