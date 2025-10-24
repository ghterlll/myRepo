package com.mobile.aura.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure a date string is not in the future.
 * The date must be today or in the past.
 * Null values are considered valid.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PastOrPresentDateValidator.class)
@Documented
public @interface PastOrPresentDate {

    String message() default "Date must not be in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
