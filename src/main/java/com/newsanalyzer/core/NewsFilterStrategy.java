package com.newsanalyzer.core;

/**
 * Strategy interface for classifying news headlines.
 */
@FunctionalInterface
public interface NewsFilterStrategy{
    boolean isPositive(String headline);
}