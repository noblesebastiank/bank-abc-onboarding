        </div>
        <div class="footer">
            <p><strong>Bank ABC</strong> - Your Trusted Financial Partner</p>
            <p>
                <#if bankWebsiteUrl??>
                    <a href="${bankWebsiteUrl}">Visit our website</a> | 
                </#if>
                <#if supportEmail??>
                    <a href="mailto:${supportEmail}">Contact Support</a> | 
                </#if>
                <#if bankPhoneNumber??>
                    Call us: ${bankPhoneNumber}
                </#if>
            </p>
            <p>
                <#if currentDate??>
                    ${currentDate}
                </#if>
                <#if currentTime??>
                    at ${currentTime}
                </#if>
            </p>
            <p style="margin-top: 20px; font-size: 11px; color: #999;">
                This email was sent by Bank ABC. Please do not reply to this email address.<br>
                If you have any questions, please contact our support team using the information above.
            </p>
            <p style="font-size: 10px; color: #ccc; margin-top: 15px;">
                Bank ABC is a registered financial institution. All rights reserved.
            </p>
        </div>
    </div>
</body>
</html>
