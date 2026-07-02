package com.newsanalyzer.core;

/**
 * Immutable result object for message validation outcomes.
 */
public final class ValidationResult{
    private static final ValidationResult VALID = new ValidationResult(true, "valid");
    private final boolean valid;
    private final String reason;

    private ValidationResult(boolean valid, String reason){
        this.valid = valid;
        this.reason = reason;
    }

    public static ValidationResult valid(){
        return VALID;
    }

    public static ValidationResult invalid(String reason){
        return new ValidationResult(false, reason);
    }

    public boolean isValid(){
        return valid;
    }

    public String getReason(){
        return reason;
    }
}
