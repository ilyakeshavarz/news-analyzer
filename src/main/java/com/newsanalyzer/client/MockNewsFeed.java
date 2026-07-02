package com.newsanalyzer.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsanalyzer.config.AppConfig;
import com.newsanalyzer.core.NewsMessage;

/**
 * Mock TCP client that generates random news messages and sends them to the analyzer.
 */
public final class MockNewsFeed{
    private static final Logger logger = LoggerFactory.getLogger(MockNewsFeed.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int MAX_PRIORITY = 9;
    private static final int PRIORITY_WEIGHT_SUM  = 55; // 10 + 9 + ... + 1

    private final String feedId;
    private final String host;
    private final int port;
    private final long intervalMillis;
    private final long reconnectInitialMillis;
    private final long reconnectMaxMillis;
    private final long reconnectJitterMillis;
    private final String[] words;
    private final AtomicBoolean running;
    private volatile Socket currentSocket;

    public MockNewsFeed(){
        this(
            AppConfig.getFeedId(),
            AppConfig.getFeedHost(),
            AppConfig.getServerPort(),
            AppConfig.getFeedIntervalMillis(),
            AppConfig.getFeedReconnectInitialMillis(),
            AppConfig.getFeedReconnectMaxMillis(),
            AppConfig.getFeedReconnectJitterMillis(),
            AppConfig.getAllowedNewsWords()
        );
    }

    public MockNewsFeed(
        String feedId,
        String host,
        int port,
        long intervalMillis,
        long reconnectInitialMillis,
        long reconnectMaxMillis,
        long reconnectJitterMillis){

        this(
            feedId,
            host,
            port,
            intervalMillis,
            reconnectInitialMillis,
            reconnectMaxMillis,
            reconnectJitterMillis,
            AppConfig.getAllowedNewsWords()
        );
    }

    public MockNewsFeed(
        String feedId,
        String host,
        int port,
        long intervalMillis,
        long reconnectInitialMillis,
        long reconnectMaxMillis,
        long reconnectJitterMillis,
        String allowedWordsCsv){

        if (feedId == null || feedId.trim().isEmpty()){
            throw new IllegalArgumentException("feedId must not be empty");
        }
        if (host == null || host.trim().isEmpty()){
            throw new IllegalArgumentException("host must not be empty");
        }
        if (port <= 0 || port > 65535){
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (intervalMillis <= 0){
            throw new IllegalArgumentException("intervalMillis must be greater than zero");
        }
        if (reconnectInitialMillis <= 0){
            throw new IllegalArgumentException("reconnectInitialMillis must be greater than zero");
        }
        if (reconnectMaxMillis < reconnectInitialMillis){
            throw new IllegalArgumentException("reconnectMaxMillis must be greater than or equal to reconnectInitialMillis");
        }
        if (reconnectJitterMillis < 0){
            throw new IllegalArgumentException("reconnectJitterMillis must not be negative");
        }

        String[] parsedWords = parseAllowedWords(allowedWordsCsv);
        if (parsedWords.length == 0){
            throw new IllegalArgumentException("allowedWordsCsv must contain at least one word");
        }

        this.feedId = feedId.trim();
        this.host = host.trim();
        this.port = port;
        this.intervalMillis = intervalMillis;
        this.reconnectInitialMillis = reconnectInitialMillis;
        this.reconnectMaxMillis = reconnectMaxMillis;
        this.reconnectJitterMillis = reconnectJitterMillis;
        this.words = parsedWords;
        this.running = new AtomicBoolean(true);
    }

    public void start(){
        int attempt = 0;

        while (running.get() && !Thread.currentThread().isInterrupted()){
            try (Socket socket = new Socket(host, port);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))){

                currentSocket = socket;
                socket.setTcpNoDelay(true);

                logger.info("{} connected to {}:{}", feedId, host, port);
                attempt = 0;

                while (running.get() && !Thread.currentThread().isInterrupted()){
                    String headline = generateHeadline();
                    int priority = generatePriority();
                    String json = MAPPER.writeValueAsString(new NewsMessage(headline, priority));

                    writer.write(json);
                    writer.newLine();
                    writer.flush();

                    logger.info("{} sent \"{}\" [p={}]", feedId, headline, priority);

                    Thread.sleep(intervalMillis);
                }
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
            } catch(IOException e){
                currentSocket = null;

                if (running.get()){
                    long delay = reconnectDelayMillis(attempt++);
                    logger.warn("{} connection failed/lost, reconnecting in {}ms: {}", feedId, delay, e.getMessage());
                    sleepBeforeReconnect(delay);
                }
            } finally{
                currentSocket = null;
            }
        }
        logger.info("{} stopped", feedId);
    }

    public void stop(){
        if (!running.compareAndSet(true, false)){
            return;
        }

        Socket socket = currentSocket;
        if (socket != null){
            try{
                socket.close();
            } catch(IOException e){
                logger.warn("Error closing feed socket: {}", e.getMessage());
            }
        }
    }

    private void sleepBeforeReconnect(long delay){
        try{
            long jitter = reconnectJitterMillis == 0 ? 0 : ThreadLocalRandom.current().nextLong(reconnectJitterMillis + 1);
            Thread.sleep(delay + jitter);
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private long reconnectDelayMillis(int attempt){
        long delay = reconnectInitialMillis;
        int cappedAttempt = Math.min(attempt, 30);

        for (int i = 0; i < cappedAttempt && delay < reconnectMaxMillis; i++){
            delay = Math.min(delay * 2, reconnectMaxMillis);
        }
        return delay;
    }

    private String generateHeadline(){
        int count = 3 + ThreadLocalRandom.current().nextInt(3);
        String[] selectedWords = new String[count];

        for (int i = 0; i < count; i++){
            selectedWords[i] = words[ThreadLocalRandom.current().nextInt(words.length)];
        }

        return String.join(" ", selectedWords);
    }

    private int generatePriority(){
        int ticket = ThreadLocalRandom.current().nextInt(PRIORITY_WEIGHT_SUM );
        int cumulative = 0;

        for (int priority = 0; priority <= MAX_PRIORITY; priority++){
            int weight = 10 - priority;
            cumulative += weight;

            if (ticket < cumulative){
                return priority;
            }
        }

        return MAX_PRIORITY;
    }

    private static String[] parseAllowedWords(String allowedWordsCsv){
        if (allowedWordsCsv == null || allowedWordsCsv.trim().isEmpty()){
            return new String[0];
        }

        return Arrays.stream(allowedWordsCsv.split(","))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .map(word -> word.toLowerCase(Locale.ROOT))
                .distinct()
                .toArray(String[]::new);
    }

    public static void main(String[] args){
        MockNewsFeed feed = new MockNewsFeed();
        Runtime.getRuntime().addShutdownHook(new Thread(feed::stop));
        feed.start();
    }
}