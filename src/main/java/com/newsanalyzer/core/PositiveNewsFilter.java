package com.newsanalyzer.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.newsanalyzer.config.AppConfig;

/**
 * Classifies headlines as positive when more than half of their words are positive.
 */
public final class PositiveNewsFilter implements NewsFilterStrategy{
    private final Set<String> positiveWords;

    public PositiveNewsFilter(){
        this.positiveWords = new HashSet<>();
        
        for (String w : AppConfig.getPositiveWords().split(",")){
            positiveWords.add(w.trim().toLowerCase());
        }
    }

    @Override
    public boolean isPositive(String headline){
        if (headline == null || headline.isEmpty()){
            return false;
        }

        String[] words = headline.toLowerCase().trim().split("\\s+");
        if (words.length == 0){
            return false;
        }

        long count = Arrays.stream(words)
            .filter(positiveWords::contains)
            .count();

        return (double) count / words.length > 0.5;
    }
}