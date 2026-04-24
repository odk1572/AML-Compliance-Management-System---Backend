package com.app.aml.feature.ingestion.batch;

import com.app.aml.domain.enums.CustomerType;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CustomerProfileValidationProcessorTest {

    private CustomerProfileValidationProcessor processor;

    @BeforeEach
    void setUp() {
        // Initialize the processor before each test
        processor = new CustomerProfileValidationProcessor();
    }

    // Helper method to generate a pristine, valid DTO
    private CustomerProfileCsvDto createValidDto() {
        CustomerProfileCsvDto dto = new CustomerProfileCsvDto();
        dto.setLineNumber(1);
        dto.setAccountNumber("ACC-998877");
        dto.setCustomerName("John Doe");
        dto.setCustomerType("INDIVIDUAL"); // Ensure this matches a real enum in your CustomerType
        dto.setIdType("PASSPORT");
        dto.setIdNumber("P12345678");
        dto.setNationality("USA");
        dto.setCountryOfResidence("USA");
        dto.setMonthlyIncome("5500.50");
        dto.setNetWorth("125000.00");
        dto.setRiskRating("LOW");
        dto.setIsPep("false");
        dto.setIsDormant("false");
        dto.setAccountOpenedOn("2024-01-15");
        return dto;
    }

    @Test
    @DisplayName("Should successfully map a fully valid DTO to a CustomerProfile entity")
    void process_ValidDto_ReturnsEntity() {
        // Arrange
        CustomerProfileCsvDto dto = createValidDto();

        // Act
        CustomerProfile result = processor.process(dto);

        // Assert
        assertNotNull(result);
        assertEquals("ACC-998877", result.getAccountNumber());
        assertEquals("John Doe", result.getCustomerName());
        assertEquals(CustomerType.valueOf("INDIVIDUAL"), result.getCustomerType());
        assertEquals(new BigDecimal("5500.50"), result.getMonthlyIncome());
        assertEquals(new BigDecimal("125000.00"), result.getNetWorth());
        assertEquals(LocalDate.of(2024, 1, 15), result.getAccountOpenedOn());
        assertFalse(result.isPep());
        assertFalse(result.isDormant());
        assertEquals("LOW", result.getRiskRating());
    }

    @Test
    @DisplayName("Should handle null numerical values safely by defaulting to ZERO")
    void process_NullDecimals_DefaultsToZero() {
        CustomerProfileCsvDto dto = createValidDto();
        dto.setMonthlyIncome(null);
        dto.setNetWorth(null);

        CustomerProfile result = processor.process(dto);

        assertEquals(BigDecimal.ZERO, result.getMonthlyIncome());
        assertEquals(BigDecimal.ZERO, result.getNetWorth());
    }

    @Test
    @DisplayName("Should throw ValidationException when Account Number is null or blank")
    void process_NullAccountNumber_ThrowsException() {
        CustomerProfileCsvDto dto = createValidDto();
        dto.setAccountNumber("   "); // Blank

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            processor.process(dto);
        });

        assertEquals(1, exception.getRow());
        assertEquals("accountNumber", exception.getColumn());
        assertTrue(exception.getMessage().contains("Cannot be null or blank"));
    }

    @Test
    @DisplayName("Should throw ValidationException when Customer Type enum is invalid")
    void process_InvalidCustomerType_ThrowsException() {
        CustomerProfileCsvDto dto = createValidDto();
        dto.setCustomerType("NON_EXISTENT_TYPE");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            processor.process(dto);
        });

        assertEquals(1, exception.getRow());
        assertEquals("customerType", exception.getColumn());
        assertTrue(exception.getMessage().contains("Invalid or missing Customer Type"));
    }

    @Test
    @DisplayName("Should throw ValidationException when numerical fields contain letters")
    void process_InvalidDecimal_ThrowsException() {
        CustomerProfileCsvDto dto = createValidDto();
        dto.setMonthlyIncome("5000.ABC"); // Bad decimal format

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            processor.process(dto);
        });

        assertEquals(1, exception.getRow());
        assertEquals("monthlyIncome/netWorth", exception.getColumn());
        assertTrue(exception.getMessage().contains("Must be a valid decimal number"));
    }

    @Test
    @DisplayName("Should throw ValidationException when date format is incorrect")
    void process_InvalidDateFormat_ThrowsException() {
        CustomerProfileCsvDto dto = createValidDto();
        dto.setAccountOpenedOn("15-01-2024"); // Expected format is YYYY-MM-DD

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            processor.process(dto);
        });

        assertEquals(1, exception.getRow());
        assertEquals("accountOpenedOn", exception.getColumn());
        assertTrue(exception.getMessage().contains("Invalid date format"));
    }
}