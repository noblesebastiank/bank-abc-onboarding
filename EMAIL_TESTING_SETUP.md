# Email Testing Setup

This project uses **smtp4dev** for local email testing during development.

## Quick Start

1. **Start smtp4dev**:
   ```bash
   docker-compose up -d
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **View captured emails**:
   - Open http://localhost:8081 in your browser
   - All outgoing emails will be displayed there

## Configuration

The application is pre-configured to use smtp4dev:

- **SMTP Host**: localhost
- **SMTP Port**: 2525
- **Web UI**: http://localhost:8081
- **Authentication**: None required

## Production

For production environments, override the mail configuration using environment variables:

```bash
export SPRING_MAIL_HOST=smtp.your-provider.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=your-username
export SPRING_MAIL_PASSWORD=your-password
export SPRING_MAIL_SMTP_AUTH=true
export SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
export SPRING_MAIL_FROM=noreply@yourdomain.com
```

## Benefits of smtp4dev

- ✅ Reliable message storage
- ✅ Clean web interface
- ✅ No authentication required for local testing
- ✅ Better error handling than alternatives
- ✅ Easy to use API
