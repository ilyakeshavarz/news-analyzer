package com.newsanalyzer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized configuration provider with property and system override support.
 */
public final class AppConfig{
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties props = new Properties();

    static{
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")){
            if (in != null){
                props.load(in);
                logger.info("Loaded application.properties successfully.");
            } else{
                logger.warn("application.properties not found. Using default configuration values.");
            }
        } catch(IOException e){
            logger.warn("Could not load application.properties. Using default configuration values.", e);
        }
    }

    private AppConfig(){
    }

    public static int getServerPort(){
        return getInt("server.port", 8888);
    }

    public static int getSummaryIntervalSeconds(){
        return getInt("summary.interval.seconds", 10);
    }

    public static int getQueueCapacity(){
        return getInt("queue.capacity", 1000);
    }

    public static int getCorePoolSize(){
        return getInt("thread.core.pool.size", 5);
    }

    public static int getMaxPoolSize(){
        return getInt("thread.max.pool.size", 20);
    }

    public static long getKeepAliveSeconds(){
        return getLong("thread.keep.alive.seconds", 60L);
    }

    public static String getPositiveWords(){
        return getString("positive.words", "up,rise,good,success,high");
    }

    public static String getAllowedNewsWords(){
        return getString("news.allowed.words", "up,down,rise,fall,good,bad,success,failure,high,low");
    }

    public static int getMinHeadlineWords(){
        return getInt("headline.min.words", 3);
    }

    public static int getMaxHeadlineWords(){
        return getInt("headline.max.words", 5);
    }

    public static String getFeedHost(){
        return getString("feed.host", "localhost");
    }

    public static String getFeedId(){
        return getString("feed.id", "feed-" + Long.toHexString(System.nanoTime()));
    }

    public static long getFeedIntervalMillis(){
        return getLong("feed.interval.millis", 1000L);
    }

    public static long getFeedReconnectInitialMillis(){
        return getLong("feed.reconnect.initial.millis", 1000L);
    }

    public static long getFeedReconnectMaxMillis(){
        return getLong("feed.reconnect.max.millis", 30000L);
    }

    public static long getFeedReconnectJitterMillis(){
        return getLong("feed.reconnect.jitter.millis", 500L);
    }

    private static int getInt(String key, int defaultValue){
        String value = System.getProperty(key, props.getProperty(key));

        if (value == null){
            logger.debug("Config key '{}' not found. Using default value: {}", key, defaultValue);
            return defaultValue;
        }

        try{
            return Integer.parseInt(value.trim());
        } catch(NumberFormatException e){
            logger.warn("Invalid integer config for key '{}': '{}'. Using default value: {}",key,value,defaultValue);
            return defaultValue;
        }
    }

    private static long getLong(String key, long defaultValue){
        String value = System.getProperty(key, props.getProperty(key));

        if (value == null){
            logger.debug("Config key '{}' not found. Using default value: {}", key, defaultValue);
            return defaultValue;
        }

        try{
            return Long.parseLong(value.trim());
        } catch(NumberFormatException e){
            logger.warn("Invalid long config for key '{}': '{}'. Using default value: {}",key,value,defaultValue);
            return defaultValue;
        }
    }

    private static String getString(String key, String defaultValue){
        String value = System.getProperty(key, props.getProperty(key));

        if (value == null){
            logger.debug("Config key '{}' not found. Using default value: {}", key, defaultValue);
            return defaultValue;
        }
        return value.trim();
    }
}