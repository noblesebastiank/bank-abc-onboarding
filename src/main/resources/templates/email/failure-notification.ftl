<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${subject}</title>
    <style>
        body { 
            font-family: Arial, sans-serif; 
            line-height: 1.6; 
            color: #333; 
            margin: 0; 
            padding: 0; 
        }
        .container { 
            max-width: 600px; 
            margin: 0 auto; 
            padding: 20px; 
        }
        .header { 
            background-color: #f4f4f4; 
            padding: 20px; 
            text-align: center; 
            border-radius: 5px 5px 0 0;
        }
        .content { 
            padding: 20px; 
            background-color: #ffffff;
            border: 1px solid #ddd;
        }
        .footer { 
            background-color: #f4f4f4; 
            padding: 20px; 
            text-align: center; 
            font-size: 12px; 
            border-radius: 0 0 5px 5px;
        }
        .error-box {
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
            padding: 15px;
            border-radius: 4px;
            margin: 15px 0;
        }
        .next-steps {
            background-color: #d1ecf1;
            border: 1px solid #bee5eb;
            color: #0c5460;
            padding: 15px;
            border-radius: 4px;
            margin: 15px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>${subject}</h2>
        </div>
        <div class="content">
            <p>Dear ${customerName},</p>
            
            <p>We encountered an issue during your onboarding process.</p>
            
            <div class="error-box">
                <strong>Issue:</strong> ${errorMessage}
            </div>
            
            <div class="next-steps">
                <strong>Next Steps:</strong>
                <ul>
                    <#if failureType == "KYC_VERIFICATION_FAILED">
                        <li>Please ensure your passport and photo documents are clear and readable</li>
                        <li>Make sure all personal information matches your official documents</li>
                        <li>Contact our support team if you need assistance</li>
                    <#elseif failureType == "ADDRESS_VERIFICATION_FAILED">
                        <li>Please ensure your address information is complete and accurate</li>
                        <li>Verify that your address is a residential address (not a P.O. Box)</li>
                        <li>Contact our support team if you need assistance updating your address</li>
                    <#elseif failureType == "ACCOUNT_CREATION_FAILED">
                        <li>Our technical team has been notified of this issue</li>
                        <li>We will attempt to resolve this within 24 hours</li>
                        <li>You will receive another notification once your account is created</li>
                        <li>Contact our support team if you need immediate assistance</li>
                    <#elseif failureType == "DOCUMENT_UPLOAD_FAILED">
                        <li>Please ensure your documents are in the correct format (JPEG, PNG, or PDF)</li>
                        <li>Make sure file sizes are under 10MB</li>
                        <li>Ensure documents are clear and all text is readable</li>
                        <li>Try uploading your documents again</li>
                    <#else>
                        <li>Please review your information and try again</li>
                        <li>Contact our support team if you need assistance</li>
                        <li>You can restart the process by logging into your account</li>
                    </#if>
                </ul>
            </div>
            
            <p>If you have any questions, please contact our support team.</p>
        </div>
        <div class="footer">
            <p>Best regards,<br>Bank ABC Team</p>
            <p>This is an automated message. Please do not reply to this email.</p>
        </div>
    </div>
</body>
</html>
