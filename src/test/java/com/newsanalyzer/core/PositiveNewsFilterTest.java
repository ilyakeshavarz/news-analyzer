package com.newsanalyzer.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for headline positivity classification.
 */
class PositiveNewsFilterTest{
    private final PositiveNewsFilter filter = new PositiveNewsFilter();

    @Test
    void shouldDetectPositiveHeadlineWhenMoreThanHalfWordsArePositive(){
        assertTrue(filter.isPositive("good rise up"));
    }

    @Test
    void shouldDetectNegativeHeadlineWhenNoWordsArePositive(){
        assertFalse(filter.isPositive("bad down fall"));
    }

    @Test
    void shouldReturnFalseWhenExactlyHalfWordsArePositive(){
        assertFalse(filter.isPositive("good rise bad fall"));
    }

    @Test
    void shouldReturnFalseWhenLessThanHalfWordsArePositive(){
        assertFalse(filter.isPositive("good bad failure"));
    }

    @Test
    void shouldHandleUppercaseWords(){
        assertTrue(filter.isPositive("GOOD RISE HIGH"));
    }

    @Test
    void shouldHandleExtraSpacesBetweenWords(){
        assertTrue(filter.isPositive("  good    rise   high  "));
    }

    @Test
    void shouldReturnFalseForEmptyHeadline(){
        assertFalse(filter.isPositive(""));
    }

    @Test
    void shouldReturnFalseForBlankHeadline(){
        assertFalse(filter.isPositive("   "));
    }

    @Test
    void shouldHandleNullHeadline(){
        assertFalse(filter.isPositive(null));
    }
}