package com.newsanalyzer.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON model for incoming news messages from clients.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NewsMessage{
    private final String headline;
    private final Integer priority;

    @JsonCreator
    public NewsMessage(@JsonProperty("headline") String headline, @JsonProperty("priority") Integer priority){
        this.headline = headline == null ? "" : headline.trim();
        this.priority = priority;
    }

    public String getHeadline(){
        return headline;
    }

    public Integer getPriority(){
        return priority;
    }

    @Override
    public String toString(){
        return "NewsMessage{" + "headline='" + headline + '\'' + ", priority=" + priority + '}';
    }
}