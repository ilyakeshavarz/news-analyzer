package com.newsanalyzer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for application configuration loading and overrides.
 */
class AppConfigTest{

    @Test
    void shouldReturnServerPort() {
        withSystemProperty("server.port", null, () -> {
            assertEquals(8888, AppConfig.getServerPort());
        });
    }

    @Test
    void shouldReturnSummaryInterval() {
        withSystemProperty("summary.interval.seconds", null, () -> {
            assertEquals(10, AppConfig.getSummaryIntervalSeconds());
        });
    }

    @Test
    void shouldReturnQueueCapacity() {
        withSystemProperty("queue.capacity", null, () -> {
            assertEquals(1000, AppConfig.getQueueCapacity());
        });
    }

    @Test
    void shouldReturnThreadPoolSettings() {
        withSystemProperty("thread.core.pool.size", null, () -> {
            assertEquals(5, AppConfig.getCorePoolSize());
        });

        withSystemProperty("thread.max.pool.size", null, () -> {
            assertEquals(20, AppConfig.getMaxPoolSize());
        });

        withSystemProperty("thread.keep.alive.seconds", null, () -> {
            assertEquals(60L, AppConfig.getKeepAliveSeconds());
        });
    }

    @Test
    void shouldReturnPositiveWords() {
        withSystemProperty("positive.words", null, () -> {
            String words = AppConfig.getPositiveWords();

            assertTrue(words.contains("up"));
            assertTrue(words.contains("rise"));
            assertTrue(words.contains("good"));
            assertTrue(words.contains("success"));
            assertTrue(words.contains("high"));
        });
    }

    @Test
    void shouldReturnAllowedNewsWords() {
        withSystemProperty("news.allowed.words", null, () -> {
            String words = AppConfig.getAllowedNewsWords();

            assertTrue(words.contains("up"));
            assertTrue(words.contains("down"));
            assertTrue(words.contains("rise"));
            assertTrue(words.contains("fall"));
            assertTrue(words.contains("good"));
            assertTrue(words.contains("bad"));
            assertTrue(words.contains("success"));
            assertTrue(words.contains("failure"));
            assertTrue(words.contains("high"));
            assertTrue(words.contains("low"));
        });
    }

    @Test
    void shouldReturnHeadlineWordLimits() {
        withSystemProperty("headline.min.words", null, () -> {
            assertEquals(3, AppConfig.getMinHeadlineWords());
        });

        withSystemProperty("headline.max.words", null, () -> {
            assertEquals(5, AppConfig.getMaxHeadlineWords());
        });
    }

    @Test
    void shouldReturnFeedSettings() {
        withSystemProperty("feed.host", null, () -> {
            assertEquals("localhost", AppConfig.getFeedHost());
        });

        withSystemProperty("feed.interval.millis", null, () -> {
            assertEquals(1000L, AppConfig.getFeedIntervalMillis());
        });

        withSystemProperty("feed.reconnect.initial.millis", null, () -> {
            assertEquals(1000L, AppConfig.getFeedReconnectInitialMillis());
        });

        withSystemProperty("feed.reconnect.max.millis", null, () -> {
            assertEquals(30000L, AppConfig.getFeedReconnectMaxMillis());
        });

        withSystemProperty("feed.reconnect.jitter.millis", null, () -> {
            assertEquals(500L, AppConfig.getFeedReconnectJitterMillis());
        });
    }

    @Test
    void shouldOverrideIntegerValueWithSystemProperty() {
        withSystemProperty("server.port", "9999", () -> {
            assertEquals(9999, AppConfig.getServerPort());
        });
    }

    @Test
    void shouldOverrideLongValueWithSystemProperty() {
        withSystemProperty("feed.interval.millis", "250", () -> {
            assertEquals(250L, AppConfig.getFeedIntervalMillis());
        });
    }

    @Test
    void shouldOverrideStringValueWithSystemProperty() {
        withSystemProperty("feed.host", "127.0.0.1", () -> {
            assertEquals("127.0.0.1", AppConfig.getFeedHost());
        });
    }

    @Test
    void shouldFallbackToDefaultWhenIntegerSystemPropertyIsInvalid() {
        withSystemProperty("server.port", "invalid", () -> {
            assertEquals(8888, AppConfig.getServerPort());
        });
    }

    @Test
    void shouldFallbackToDefaultWhenLongSystemPropertyIsInvalid() {
        withSystemProperty("feed.interval.millis", "invalid", () -> {
            assertEquals(1000L, AppConfig.getFeedIntervalMillis());
        });
    }

    private void withSystemProperty(String key, String value, Runnable testBody){
        String previous = System.getProperty(key);

        try{
            if (value == null){
                System.clearProperty(key);
            } else{
                System.setProperty(key, value);
            }

            testBody.run();
        } finally{
            if (previous == null){
                System.clearProperty(key);
            } else{
                System.setProperty(key, previous);
            }
        }
    }
}