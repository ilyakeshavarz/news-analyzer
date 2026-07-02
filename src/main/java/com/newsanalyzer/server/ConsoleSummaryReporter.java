package com.newsanalyzer.server;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import com.newsanalyzer.core.NewsItem;

/**
 * Console implementation for displaying summary results.
 */
public final class ConsoleSummaryReporter implements SummaryReporter{
    @Override
    public void report(SummaryResult result, int summaryIntervalSeconds, long timestampMillis){
        Objects.requireNonNull(result, "summary must not be null");

        String sep = repeat('=', 50);
        String dash = repeat('-', 50);

        System.out.println("\n" + sep);
        System.out.printf("SUMMARY [Last %ds] - %tT%n", summaryIntervalSeconds, new Date(timestampMillis));
        System.out.println(dash);
        System.out.println("Positive count: " + result.getPositiveCount());

        if (!result.getTopItems().isEmpty()){
            System.out.println("Top headlines:");
            int rank = 0;
            for (NewsItem item : result.getTopItems()) {
                System.out.printf("   %d. \"%s\" [p=%d]%n", ++rank, item.getHeadline(), item.getPriority());
            }
        } else{
            System.out.println("No positive news in this window.");
        }

        System.out.println(sep);
    }

    private static String repeat(char ch, int count){
        if (count < 0){
            throw new IllegalArgumentException("count must not be negative");
        }

        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }
}