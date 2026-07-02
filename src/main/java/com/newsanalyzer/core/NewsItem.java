package com.newsanalyzer.core;
import java.util.Objects;

/**
 * Internal model for validated positive news items.
 */
public final class NewsItem {
    private final String headline;
    private final int priority;
    private final long timestamp;

    public NewsItem(String headline, int priority){
        this(headline, priority, System.currentTimeMillis());
    }

    public NewsItem(String headline, int priority, long timestamp) {
        if (headline == null || headline.trim().isEmpty()) {
            throw new IllegalArgumentException("headline must not be blank");
        }
        if (priority < 0 || priority > 9) {
            throw new IllegalArgumentException("priority must be between 0 and 9");
        }

        this.headline = headline;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    public String getHeadline(){
        return headline;
    }

    public int getPriority(){
        return priority;
    }

    public long getTimestamp(){
        return timestamp;
    }

    @Override
    public boolean equals(Object object){
        if (this == object) {
            return true;
        }
        if (!(object instanceof NewsItem)){
            return false;
        }
        NewsItem newsItem = (NewsItem) object;
        return priority == newsItem.priority && timestamp == newsItem.timestamp && Objects.equals(headline, newsItem.headline);
    }

    @Override
    public int hashCode(){
        return Objects.hash(headline, priority, timestamp);
    }

    @Override
    public String toString(){
        return "NewsItem{" + "headline='" + headline + '\'' + ", priority=" + priority + ", timestamp=" + timestamp + '}';
    }
}
