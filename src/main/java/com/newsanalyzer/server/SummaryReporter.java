package com.newsanalyzer.server;

/**
 * Abstraction for reporting generated news summaries.
 */
@FunctionalInterface
public interface SummaryReporter{
    void report(SummaryResult result, int resultIntervalSeconds, long timestampMillis);
}
