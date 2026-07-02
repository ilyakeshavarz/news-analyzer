package com.newsanalyzer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for validating incoming news message rules.
 */
class NewsMessageValidatorTest{
    private final NewsMessageValidator validator = new NewsMessageValidator("up,down,rise,fall,good,bad,success,failure,high,low",3,5);

    @Test
    void shouldAcceptValidNewsMessage(){
        ValidationResult result = validator.validate(new NewsMessage("up good success", 8));
        assertTrue(result.isValid());
        assertEquals("valid", result.getReason());
    }

    @Test
    void shouldRejectNullMessage(){
        ValidationResult result = validator.validate(null);

        assertFalse(result.isValid());
        assertEquals("message is null", result.getReason());
    }

    @Test
    void shouldRejectNegativePriority(){
        ValidationResult result = validator.validate(new NewsMessage("up good success", -1));

        assertFalse(result.isValid());
        assertEquals("priority must be between 0 and 9", result.getReason());
    }
    @Test
    void shouldRejectMissingPriority(){
        ValidationResult result = validator.validate(new NewsMessage("up good success", null));

        assertFalse(result.isValid());
        assertEquals("priority is required", result.getReason());
    }

    @Test
    void shouldRejectPriorityGreaterThanNine(){
        ValidationResult result = validator.validate(new NewsMessage("up good success", 10));

        assertFalse(result.isValid());
        assertEquals("priority must be between 0 and 9", result.getReason());
    }

    @Test
    void shouldRejectEmptyHeadline(){
        ValidationResult result = validator.validate(new NewsMessage("", 5));

        assertFalse(result.isValid());
        assertEquals("headline must not be empty", result.getReason());
    }

    @Test
    void shouldRejectBlankHeadline(){
        ValidationResult result = validator.validate(new NewsMessage("   ", 5));

        assertFalse(result.isValid());
        assertEquals("headline must not be empty", result.getReason());
    }

    @Test
    void shouldRejectTooShortHeadline(){
        ValidationResult result = validator.validate(new NewsMessage("up good", 5));

        assertFalse(result.isValid());
        assertEquals("headline must contain 3 to 5 words", result.getReason());
    }

    @Test
    void shouldRejectTooLongHeadline(){
        ValidationResult result = validator.validate(new NewsMessage("up good high rise success fall", 5));

        assertFalse(result.isValid());
        assertEquals("headline must contain 3 to 5 words", result.getReason());
    }

    @Test
    void shouldRejectUnsupportedWords(){
        ValidationResult result = validator.validate(new NewsMessage("up good unknown", 5));

        assertFalse(result.isValid());
        assertEquals("headline contains unsupported word: unknown", result.getReason());
    }

    @Test
    void shouldAcceptUppercaseWordsBecauseValidationIsCaseInsensitive(){
        ValidationResult result = validator.validate(new NewsMessage("UP Good SUCCESS", 5));

        assertTrue(result.isValid());
    }

}