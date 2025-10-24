package com.mobile.aura.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Validator for {@link PastOrPresentDate} annotation.
 * Validates that a date string (in format "yyyy-MM-dd") is not in the future.
 */
public class PastOrPresentDateValidator implements ConstraintValidator<PastOrPresentDate, String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are considered valid (use @NotNull for null checks)
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
            LocalDate today = LocalDate.now();

            // Date must be today or in the past
            return !date.isAfter(today);
        } catch (DateTimeParseException e) {
            // Invalid date format will be caught by @Pattern validation
            return true;
        }
    }
}
