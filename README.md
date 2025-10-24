# Bank ABC Digital Customer Onboarding

A Spring Boot microservice for multi-step digital customer onboarding with Camunda BPMN workflow orchestration.

## Overview

This microservice enables customers to complete their bank account onboarding process online through a multi-step workflow. The process is orchestrated using Camunda BPMN engine, ensuring reliable and trackable customer onboarding.

## Features

- **Multi-step Onboarding Process**: Collect customer information and upload documents in separate steps
- **BPMN Workflow Orchestration**: Uses Camunda BPMN engine for process management
- **Document Upload**: Support for passport and photo document uploads
- **KYC Verification**: Know Your Customer verification process
- **Address Verification**: Customer address validation
- **Account Creation**: Automatic bank account creation with IBAN
- **Customer Notifications**: Email and SMS notifications
- **Process Tracking**: Real-time status tracking and progress monitoring
- **File Storage**: Local file storage for uploaded documents
- **RESTful API**: Complete REST API with OpenAPI documentation

## Architecture

### BPMN Workflow

The onboarding process follows this workflow:

1. **Collect Customer Information** → Wait for documents
2. **Upload Documents** → KYC Verification → Address Verification → Account Creation → Notify Customer → End

### Components

- **Controllers**: REST endpoints for workflow operations
- **Services**: Business logic and BPMN process management
- **Delegates**: Camunda BPMN task delegates for each workflow step
- **Entities**: JPA entities for data persistence
- **DTOs**: Data transfer objects for API communication
- **File Storage**: Service for handling document uploads

## API Endpoints

### Start Onboarding Process
```
POST /api/v1/onboarding/start
```
Initiates the onboarding process with customer information.

### Upload Documents
```
POST /api/v1/onboarding/{processInstanceId}/documents
```
Uploads passport and photo documents to resume the workflow.

### Get Process Status
```
GET /api/v1/onboarding/{processInstanceId}/status
```
Retrieves current status and progress of the onboarding process.

### Health Check
```
GET /api/v1/onboarding/health
```
Simple health check endpoint.

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.6+
- Docker & Docker Compose (for smtp4dev email testing)
- H2 Database (included)

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd bank-abc-onboarding
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Camunda Cockpit: http://localhost:8080/camunda
   - H2 Console: http://localhost:8080/h2-console

### Default Credentials

- **Camunda Admin**: admin/admin
- **H2 Database**: sa/password

## Email Testing with smtp4dev

This application uses smtp4dev for local email testing. smtp4dev is a development SMTP server that captures all outgoing emails and provides a web interface to view them.

### Starting smtp4dev

```bash
# Start smtp4dev for email testing
docker-compose up -d

# Check if it's running
docker-compose ps
```

### Accessing smtp4dev

- **Web UI**: http://localhost:8081
- **SMTP Server**: localhost:2525

### Email Configuration

The application is pre-configured to use smtp4dev for local development:

```yaml
spring:
  mail:
    host: localhost
    port: 2525
    username: 
    password: 
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
    from: noreply@bankabc.local
```

### Testing Email Functionality

1. **Start smtp4dev**:
   ```bash
   docker-compose up -d
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Trigger email sending** by completing onboarding steps that send notifications

4. **View captured emails** at http://localhost:8081

### Production Email Configuration

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

## Usage Examples

### 1. Start Onboarding Process

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/start \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Emma",
    "lastName": "de Vries",
    "gender": "F",
    "dob": "1990-05-20",
    "phone": "+31612345678",
    "email": "emma.devries@example.com",
    "nationality": "Dutch",
    "street": "Keizersgracht 1",
    "city": "Amsterdam",
    "postalCode": "1015CD",
    "country": "Netherlands",
    "ssn": "123-45-6789"
  }'
```

**Response:**
```json
{
  "processInstanceId": "12345",
  "status": "INFO_COLLECTED",
  "message": "Onboarding process started successfully",
  "createdAt": "2024-01-15T10:30:00Z",
  "nextStep": "document_upload",
  "nextStepDescription": "Please upload your passport and photo documents"
}
```

### 2. Upload Documents

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/12345/documents \
  -F "passport=@passport.pdf" \
  -F "photo=@photo.jpg"
```

**Response:**
```json
{
  "processInstanceId": "12345",
  "status": "DOCUMENTS_UPLOADED",
  "message": "Documents uploaded successfully",
  "uploadedAt": "2024-01-15T10:35:00Z",
  "nextStep": "kyc_verification",
  "nextStepDescription": "Documents processed, verification in progress",
  "passportUploaded": true,
  "photoUploaded": true
}
```

### 3. Check Process Status

```bash
curl http://localhost:8080/api/v1/onboarding/12345/status
```

**Response:**
```json
{
  "processInstanceId": "12345",
  "status": "KYC_IN_PROGRESS",
  "message": "KYC verification in progress",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z",
  "kycVerified": false,
  "addressVerified": false,
  "currentStep": "KYC verification in progress",
  "nextStep": "address_verification",
}
```

## Configuration

### Application Properties

```yaml
# Camunda BPMN Configuration
camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    webapp:
      enabled: true
    database:
      schema-update: true

# File Storage Configuration
app:
  file-storage:
    path: /tmp/onboarding-documents
    max-size: 10485760  # 10MB
```

### Supported File Types

- **Passport**: PDF files
- **Photo**: JPEG, PNG files
- **Maximum file size**: 10MB per file

## Process Status Flow

1. **INITIATED** → Process started
2. **INFO_COLLECTED** → Customer information collected
3. **WAITING_FOR_DOCUMENTS** → Waiting for document upload
4. **DOCUMENTS_UPLOADED** → Documents uploaded successfully
5. **KYC_IN_PROGRESS** → KYC verification in progress
6. **KYC_COMPLETED** → KYC verification completed
7. **ADDRESS_VERIFICATION_IN_PROGRESS** → Address verification in progress
8. **ADDRESS_VERIFICATION_COMPLETED** → Address verification completed
9. **ACCOUNT_CREATION_IN_PROGRESS** → Creating bank account
10. **ACCOUNT_CREATED** → Bank account created
11. **NOTIFICATION_SENT** → Customer notification sent
12. **COMPLETED** → Onboarding completed successfully
13. **FAILED** → Onboarding failed

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/bankabc/onboarding/
│   │   ├── controller/          # REST controllers
│   │   ├── delegate/           # Camunda BPMN delegates
│   │   ├── dto/               # Data transfer objects
│   │   ├── entity/            # JPA entities
│   │   ├── service/           # Business services
│   │   └── OnboardingApplication.java
│   └── resources/
│       ├── processes/         # BPMN process definitions
│       ├── openapi.yaml      # OpenAPI specification
│       └── application.yml   # Application configuration
└── test/                     # Test classes
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OnboardingWorkflowControllerTest

# Run with coverage
mvn test jacoco:report
```

### Code Generation

The project uses OpenAPI code generation for DTOs and interfaces:

```bash
mvn clean compile
```

This generates the OpenAPI models and interfaces in the `target/generated-sources/openapi` directory.

## Monitoring and Management

### Camunda Cockpit

Access the Camunda Cockpit at http://localhost:8080/camunda to:
- Monitor running process instances
- View process definitions
- Manage tasks and jobs
- Analyze process performance

### Health Endpoints

- **Actuator Health**: `/actuator/health`

