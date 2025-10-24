# 🚀 Bank ABC Onboarding API - Postman Setup Guide

This guide will help you set up and use the Postman collection and environment for testing the Bank ABC Digital Customer Onboarding API.

## 📁 Files Included

- `Bank-ABC-Onboarding.postman_collection.json` - Complete Postman collection with all API requests
- `Bank-ABC-Onboarding.postman_environment.json` - Environment variables for different configurations
- `POSTMAN_SETUP.md` - This setup guide

## 🛠️ Setup Instructions

### 1. Import Collection and Environment

1. **Open Postman**
2. **Import Collection:**
   - Click "Import" button
   - Select `Bank-ABC-Onboarding.postman_collection.json`
   - Click "Import"

3. **Import Environment:**
   - Click "Import" button
   - Select `Bank-ABC-Onboarding.postman_environment.json`
   - Click "Import"

4. **Select Environment:**
   - In the top-right corner, select "Bank ABC Onboarding Environment" from the dropdown

### 2. Start the Application

Before running the tests, make sure the Bank ABC Onboarding application is running:

```bash
cd /Users/nsebast/bank-abc-onboarding
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 3. Prepare Test Documents

For the document upload tests, you'll need:
- **Passport Document**: A PDF file (max 5MB)
- **Photo**: A JPEG or PNG image (max 2MB)

## 🧪 Testing Workflow

### Complete Onboarding Flow

1. **Health Check** - Verify the service is running
2. **Start Onboarding Process** - Initiate the workflow
3. **Upload Documents** - Upload passport and photo
4. **Check Onboarding Status** - Monitor progress (repeat as needed)

### Error Scenarios

- **Duplicate Customer** - Test with existing SSN
- **Invalid Request** - Test with invalid data
- **Process Not Found** - Test with invalid process ID

### Additional Endpoints

- **Swagger UI** - API documentation
- **H2 Database Console** - Database inspection
- **Camunda Cockpit** - BPMN process monitoring

## 🔧 Environment Variables

The environment includes the following variables:

| Variable | Value | Description |
|----------|-------|-------------|
| `baseUrl` | `http://localhost:8080` | API base URL |
| `processInstanceId` | (auto-set) | Process instance ID from responses |
| `testFirstName` | `Emma` | Test customer first name |
| `testLastName` | `de Vries` | Test customer last name |
| `testEmail` | `emma.devries@example.com` | Test customer email |
| `testPhone` | `+31612345678` | Test customer phone |
| `testSsn` | `123-45-6789` | Test customer SSN |
| `testDob` | `1990-05-20` | Test customer date of birth |
| `testGender` | `F` | Test customer gender |
| `testNationality` | `Dutch` | Test customer nationality |
| `testStreet` | `Keizersgracht 1` | Test customer street address |
| `testCity` | `Amsterdam` | Test customer city |
| `testPostalCode` | `1015CD` | Test customer postal code |
| `testCountry` | `Netherlands` | Test customer country |
| `h2JdbcUrl` | `jdbc:h2:mem:onboardingdb` | H2 database JDBC URL |
| `h2Username` | `sa` | H2 database username |
| `h2Password` | `password` | H2 database password |
| `camundaUsername` | `admin` | Camunda username |
| `camundaPassword` | `admin` | Camunda password |

## 🎯 Running Tests

### Individual Requests

1. Select any request from the collection
2. Click "Send" to execute
3. Check the response and test results

### Collection Runner

1. Click on the collection name
2. Click "Run" button
3. Select the requests you want to run
4. Click "Run Bank ABC Onboarding API"

### Automated Testing

The collection includes comprehensive test scripts that will:
- Validate response status codes
- Check response structure
- Verify required fields
- Test response times
- Log progress information

## 📊 Expected Workflow Statuses

The onboarding process follows these statuses:

1. `INFO_COLLECTED` - Initial data collected
2. `DOCUMENTS_UPLOADED` - Documents uploaded successfully
3. `KYC_IN_PROGRESS` - KYC verification in progress
4. `KYC_COMPLETED` - KYC verification completed
5. `ADDRESS_VERIFICATION_IN_PROGRESS` - Address verification in progress
6. `ADDRESS_VERIFICATION_COMPLETED` - Address verification completed
7. `ACCOUNT_CREATION_IN_PROGRESS` - Account creation in progress
8. `ACCOUNT_CREATED` - Account created successfully
9. `NOTIFICATION_SENT` - Customer notification sent
10. `COMPLETED` - Onboarding completed successfully

## 🔍 Monitoring and Debugging

### H2 Database Console

1. Navigate to `http://localhost:8080/h2-console`
2. Use the credentials from environment variables
3. Inspect the `ONBOARDING` table for data

### Camunda Cockpit

1. Navigate to `http://localhost:8080/camunda`
2. Use the credentials from environment variables
3. Monitor BPMN process execution

### Swagger UI

1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Explore API documentation
3. Test endpoints interactively

## 🚨 Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure the application is running on port 8080
   - Check if the port is available

2. **404 Not Found**
   - Verify the base URL is correct
   - Check if the application context path is correct

3. **Document Upload Fails**
   - Ensure files are within size limits
   - Check file formats (PDF for passport, JPEG/PNG for photo)

4. **Process Not Found**
   - Ensure you're using the correct process instance ID
   - Check if the process is still active

### Logs

Check the application logs for detailed error information:
- Console output when running `mvn spring-boot:run`
- Log files in the application directory

## 📝 Customization

### Adding New Tests

1. Duplicate an existing request
2. Modify the request details
3. Update the test scripts
4. Add to the collection

### Modifying Environment Variables

1. Select the environment
2. Edit variable values
3. Save changes

### Creating New Environments

1. Create a new environment
2. Copy variables from the existing environment
3. Modify values as needed
4. Save and select the new environment

## 🎉 Success Indicators

A successful onboarding flow should show:
- ✅ All requests return expected status codes
- ✅ Process instance ID is generated and used correctly
- ✅ Documents are uploaded successfully
- ✅ Status progresses through all stages
- ✅ Final status is `COMPLETED`
- ✅ Account number is generated
- ✅ All test assertions pass

## 📞 Support

For issues or questions:
1. Check the application logs
2. Verify environment variables
3. Ensure the application is running
4. Check the Swagger UI for API documentation

---

**Happy Testing! 🚀**
