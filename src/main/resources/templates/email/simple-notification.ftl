<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${subject!""}</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f4f4f4; padding: 20px; text-align: center; }
        .content { padding: 20px; }
        .footer { background-color: #f4f4f4; padding: 20px; text-align: center; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>${subject!""}</h2>
        </div>
        <div class="content">
            <p>Dear ${customerName!"Valued Customer"},</p>
            <p>${message!""}</p>
        </div>
        <div class="footer">
            <p>Best regards,<br>Bank ABC Team</p>
        </div>
    </div>
</body>
</html>
