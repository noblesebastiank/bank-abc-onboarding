package com.bankabc.onboarding.service;

import com.bankabc.onboarding.exception.DefaultApiError;
import com.bankabc.onboarding.exception.ErrorTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

import com.bankabc.onboarding.constants.ApplicationConstants;

/**
 * Service for generating unique IBAN-style account numbers.
 * Implements Dutch IBAN format: NL + 2 check digits + 4 bank code + 10 account digits.
 * Future enhancements:
 * - Integrate with core banking system for real account creation
 * - Implement proper IBAN checksum validation
 * - Add account number uniqueness validation
 * - Support multiple account types
 */
@Service
@Slf4j
public class AccountService {

    private static final String BANK_CODE = ApplicationConstants.Banking.BANK_CODE;
    private static final String COUNTRY_CODE = ApplicationConstants.Banking.COUNTRY_CODE;
    private final Random random = new Random();

    /**
     * Generates a unique IBAN-style account number.
     * Format: NL + 2 check digits + BANK + 10 account digits
     * 
     * @return generated account number
     */
    private String generateAccountNumber() {
        log.info("Generating new account number");
        
        // Generate 10-digit account number
        String accountDigits = generateAccountDigits();
        
        // Generate check digits (simplified for demo)
        String checkDigits = generateCheckDigits(accountDigits);
        
        // Construct IBAN-style account number
        String accountNumber = COUNTRY_CODE + checkDigits + BANK_CODE + accountDigits;
        
        log.info("Generated account number: [REDACTED]");
        return accountNumber;
    }

    /**
     * Generates 10-digit account number.
     * 
     * @return 10-digit account number as string
     */
    private String generateAccountDigits() {
        // Generate random 10-digit number
        long accountNumber = 1000000000L + random.nextLong(9000000000L);
        return String.format("%010d", accountNumber);
    }

    /**
     * Generates 2-digit check digits for IBAN.
     * This is a simplified implementation for demo purposes.
     * In production, proper IBAN checksum algorithm should be used.
     * 
     * @param accountDigits the account digits
     * @return 2-digit check digits
     */
    private String generateCheckDigits(String accountDigits) {
        // Simplified check digit generation for demo
        // In production, use proper IBAN checksum algorithm (mod 97)
        int sum = 0;
        for (char c : accountDigits.toCharArray()) {
            sum += Character.getNumericValue(c);
        }
        
        // Generate check digits based on sum
        int checkDigits = (sum % 97) + 1;
        return String.format("%02d", checkDigits);
    }

    /**
     * Creates a new bank account for the customer.
     * 
     * @param firstName the customer's first name
     * @param lastName the customer's last name
     * @param email the customer's email
     * @param phone the customer's phone number
     * @param dateOfBirth the customer's date of birth
     * @param ssn the customer's social security number
     * @return the generated account number
     * @throws DefaultApiError if account creation fails
     */
    public String createAccount(String firstName, String lastName, String email, 
                              String phone, java.time.LocalDate dateOfBirth, String ssn) {
        log.info("Creating bank account for customer");
        
        try {
            // Generate account number
            String accountNumber = generateAccountNumber();
            
            // In a real implementation, this would:
            // 1. Create account in core banking system
            // 2. Set up account permissions and limits
            // 3. Generate account statements
            // 4. Set up online banking access
            // 5. Create account agreement documents
            
            log.info("Bank account created successfully");
            return accountNumber;
            
        } catch (Exception e) {
            log.error("Unexpected error creating bank account", e);
            throw new DefaultApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorTypes.ACCOUNT_CREATION_FAILED.name(),
                ErrorTypes.ACCOUNT_CREATION_FAILED.getMessage(),
                Map.of(
                    "firstName", firstName != null ? firstName : "null",
                    "lastName", lastName != null ? lastName : "null",
                    "originalError", e.getMessage()
                )
            );
        }
    }

}

