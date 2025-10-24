package com.bankabc.onboarding.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration class for email services.
 * Configures JavaMailSender and Freemarker template engine.
 */
@org.springframework.context.annotation.Configuration
public class EmailConfig {

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:2525}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:}")
    private String sslTrust;

    @Value("${spring.mail.from:noreply@bankabc.com}")
    private String mailFrom;

    /**
     * Configures JavaMailSender bean.
     * 
     * @return configured JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        
        if (sslTrust != null && !sslTrust.isEmpty()) {
            props.put("mail.smtp.ssl.trust", sslTrust);
        }
        
        // Additional properties for better compatibility
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "3000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Configures Freemarker template engine.
     * 
     * @return configured Freemarker Configuration
     */
    @Bean
    public Configuration freemarkerConfig() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_32);
        
        // Set template loading path
        config.setClassForTemplateLoading(this.getClass(), "/templates/email");
        
        // Set default encoding
        config.setDefaultEncoding("UTF-8");
        
        // Set template exception handler
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        // Set log template exceptions
        config.setLogTemplateExceptions(false);
        
        // Set wrap unchecked exceptions
        config.setWrapUncheckedExceptions(true);
        
        return config;
    }


}
