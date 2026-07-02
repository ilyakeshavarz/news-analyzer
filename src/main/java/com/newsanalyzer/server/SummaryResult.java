package com.newsanalyzer.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.newsanalyzer.core.NewsItem;

/**
 * Immutable result of a generated summary window.
 */
public final class SummaryResult{
    private final int positiveCount;
    private final List<NewsItem> topItems;

    public SummaryResult(int positiveCount, List<NewsItem> topItems){
        if (positiveCount < 0){
            throw new IllegalArgumentException("positiveCount must not be negative");
        }
        if (topItems == null){
            throw new IllegalArgumentException("topItems must not be null");
        }

        this.positiveCount = positiveCount;
        this.topItems = Collections.unmodifiableList(new ArrayList<>(topItems));
    
    }

    public int getPositiveCount(){
        return positiveCount;
    }

    public List<NewsItem> getTopItems(){
        return topItems;
    }

    @Override
    public String toString() {
        return "SummaryResult{" + "positiveCount=" + positiveCount + ", topItems=" + topItems + '}';
}
}
