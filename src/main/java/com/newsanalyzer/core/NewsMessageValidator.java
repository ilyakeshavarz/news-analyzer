package com.newsanalyzer.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.newsanalyzer.config.AppConfig;

/**
 * Validates incoming news messages before processing.
 */
public final class NewsMessageValidator{
    private final Set<String> allowedWords;
    private final int minHeadlineWords;
    private final int maxHeadlineWords;

    public NewsMessageValidator(){
        this(AppConfig.getAllowedNewsWords(), AppConfig.getMinHeadlineWords(), AppConfig.getMaxHeadlineWords());
    }

    public NewsMessageValidator(String allowedWordsCsv, int minHeadlineWords, int maxHeadlineWords) {
        if (minHeadlineWords <= 0){
            throw new IllegalArgumentException("minHeadlineWords must be greater than zero");
        }
        if (maxHeadlineWords < minHeadlineWords){
            throw new IllegalArgumentException("maxHeadlineWords must be greater than or equal to minHeadlineWords");
        }

        this.allowedWords = parseAllowedWords(allowedWordsCsv);
        this.minHeadlineWords = minHeadlineWords;
        this.maxHeadlineWords = maxHeadlineWords;
    }

    public ValidationResult validate(NewsMessage message) {
        if (message == null){
            return ValidationResult.invalid("message is null");
        }
        if (message.getPriority() == null){
            return ValidationResult.invalid("priority is required");
        }
        if (message.getPriority() < 0 || message.getPriority() > 9){
            return ValidationResult.invalid("priority must be between 0 and 9");
        }
        
        String headline = message.getHeadline();

        if (headline == null || headline.trim().isEmpty()){
            return ValidationResult.invalid("headline must not be empty");
        }

        String[] words = headline.trim().toLowerCase(Locale.ROOT).split("\\s+");

        if (words.length < minHeadlineWords || words.length > maxHeadlineWords){
            return ValidationResult.invalid("headline must contain " + minHeadlineWords + " to " + maxHeadlineWords + " words");
        }

        for (String word : words) {
            if (!allowedWords.contains(word)){
                return ValidationResult.invalid("headline contains unsupported word: " + word);
            }
        }
        return ValidationResult.valid();
    }

    private Set<String> parseAllowedWords(String str) {
        Set<String> words = new HashSet<>();
        if (str != null) {
            Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .forEach(words::add);
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("allowed words must not be empty");
        }
        return words;
    }
}
