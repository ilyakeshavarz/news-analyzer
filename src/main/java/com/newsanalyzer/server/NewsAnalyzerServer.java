package com.newsanalyzer.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsanalyzer.config.AppConfig;
import com.newsanalyzer.core.NewsFilterStrategy;
import com.newsanalyzer.core.NewsItem;
import com.newsanalyzer.core.NewsMessage;
import com.newsanalyzer.core.NewsMessageValidator;
import com.newsanalyzer.core.PositiveNewsFilter;
import com.newsanalyzer.core.ValidationResult;
import com.newsanalyzer.util.NamedThreadFactory;

/**
 * Multi-client TCP server for receiving, validating, filtering, and summarizing news.
 */
public class NewsAnalyzerServer {
    private static final Logger logger = LoggerFactory.getLogger(NewsAnalyzerServer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int port;
    private final int summaryInterval;
    private final BlockingQueue<NewsItem> queue;
    private final ThreadPoolExecutor workerPool;
    private final ScheduledExecutorService scheduler;
    private final NewsFilterStrategy filter;
    private final NewsMessageValidator validator;
    private final SummaryGenerator summaryGenerator;
    private final SummaryReporter reporter;
    private final AtomicBoolean running;
    private final CountDownLatch started;
    private final Set<Socket> clients;
    private volatile ServerSocket serverSocket;

    public NewsAnalyzerServer(){
        this(
            AppConfig.getServerPort(),
            AppConfig.getSummaryIntervalSeconds(),
            new LinkedBlockingQueue<NewsItem>(AppConfig.getQueueCapacity()),
            AppConfig.getCorePoolSize(),
            AppConfig.getMaxPoolSize(),
            AppConfig.getKeepAliveSeconds(),
            new PositiveNewsFilter(),
            new NewsMessageValidator(),
            new ConsoleSummaryReporter()
        );
    }
    // for Testing
    public NewsAnalyzerServer(int port, int summaryIntervalSeconds, SummaryReporter reporter){
        this(
            port,
            summaryIntervalSeconds,
            new LinkedBlockingQueue<NewsItem>(AppConfig.getQueueCapacity()),
            AppConfig.getCorePoolSize(),
            AppConfig.getMaxPoolSize(),
            AppConfig.getKeepAliveSeconds(),
            new PositiveNewsFilter(),
            new NewsMessageValidator(),
            reporter
        );
    }

    NewsAnalyzerServer(
        int port,
        int summaryIntervalSeconds,
        BlockingQueue<NewsItem> queue,
        int corePoolSize,
        int maxPoolSize,
        long keepAliveSeconds,
        NewsFilterStrategy filter,
        NewsMessageValidator validator,
        SummaryReporter reporter){

        if (summaryIntervalSeconds <= 0){
            throw new IllegalArgumentException("summaryIntervalSeconds must be greater than zero");
        }
        if (queue == null){
            throw new IllegalArgumentException("queue must not be null");
        }
        if (filter == null){
            throw new IllegalArgumentException("filter must not be null");
        }
        if (validator == null){
            throw new IllegalArgumentException("validator must not be null");
        }
        if (reporter == null){
            throw new IllegalArgumentException("reporter must not be null");
        }

        this.port = port;
        this.summaryInterval = summaryIntervalSeconds;
        this.queue = queue;
        this.filter = filter;
        this.validator = validator;
        this.reporter = reporter;
        this.running = new AtomicBoolean(true);
        this.started = new CountDownLatch(1);
        this.clients = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());

        this.workerPool = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new NamedThreadFactory("worker"),
            new ThreadPoolExecutor.AbortPolicy()
        );

        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("scheduler"));
        this.summaryGenerator = new SummaryGenerator(queue, summaryIntervalSeconds);
    }

    public void start() throws IOException{
        serverSocket = new ServerSocket(port);
        started.countDown(); //for Testing

        scheduler.scheduleAtFixedRate(this::printSummary, summaryInterval, summaryInterval, TimeUnit.SECONDS);

        logger.info("Server started on port {}", getPort());
        printBanner();

        while (running.get()){
            try{
                Socket client = serverSocket.accept();
                client.setTcpNoDelay(true);
                clients.add(client);
                logger.info("Client connected: {}", client.getRemoteSocketAddress());
                dispatchClient(client);
            } catch (IOException e){
                if (running.get()){
                    logger.error("Accept error: {}", e.getMessage());
                }
            }
        }
    }

    public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        return started.await(timeout, unit);
    }

    public int getPort() {
        ServerSocket socket = serverSocket;
        if (socket != null) {
            return socket.getLocalPort();
        }
        return port;
    }

    private void dispatchClient(Socket client){
        try{
            workerPool.execute(() -> handleClient(client));
        } catch(RejectedExecutionException e){
            logger.warn("Too many concurrent clients. Rejecting connection from {}", client.getRemoteSocketAddress());
            clients.remove(client);
            closeQuietly(client);
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))){
            String line;
            while (running.get() && (line = reader.readLine()) != null){
                processMessage(line);
            }
        } catch(IOException e){
            if (running.get()){
                logger.error("Client error: {}", e.getMessage());
            }
        } finally{
            clients.remove(socket);
            closeQuietly(socket);
            logger.info("Client disconnected");
        }
    }

    private void processMessage(String line){
        try{
            NewsMessage msg = MAPPER.readValue(line, NewsMessage.class);
            ValidationResult validation = validator.validate(msg);
            if (!validation.isValid()){
                logger.warn("Invalid message dropped: {} | reason={}", line, validation.getReason());
                return;
            }

            if (filter.isPositive(msg.getHeadline())){
                NewsItem item = new NewsItem(msg.getHeadline(), msg.getPriority());
                boolean added = queue.offer(item);
                if (!added){
                    logger.warn("Queue full, dropping message: {}", msg.getHeadline());
                } else{
                    logger.debug("Positive news accepted: {}", msg.getHeadline());
                }
            }
        } catch(IOException e){
            logger.warn("Parse error, invalid JSON dropped: {}", line);
        }
    }

    private void printSummary(){
        try{
            long now = System.currentTimeMillis();
            SummaryResult result = summaryGenerator.generate(now);
            reporter.report(result, summaryInterval, now);
        } catch(RuntimeException e){
            logger.error("Failed to generate or report summary", e);
    }

    }

    private void printBanner(){
        System.err.println("");
        System.err.println("<><><><><><><><><><><><><><><><>");
        System.out.println("   News Analyzer v2.1.0");
        System.out.println("   Port: " + getPort());
        System.out.println("   Summary interval: " + summaryInterval + "s");
        System.err.println("<><><><><><><><><><><><><><><><>");
        System.err.println("");
    }

    public void stop(){
        if (!running.compareAndSet(true, false)){
            return;
        }

        closeQuietly(serverSocket);
        closeAllClients();

        workerPool.shutdownNow();
        scheduler.shutdownNow();

        try{
            workerPool.awaitTermination(10, TimeUnit.SECONDS);
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }

        logger.info("Server stopped");
    }

    private void closeAllClients(){
        for (Socket client : clients){
            closeQuietly(client);
        }
        clients.clear();
    }

    private void closeQuietly(ServerSocket socket){
        if (socket == null){
            return;
        }
        try {
            socket.close();
        } catch (IOException e){
            logger.warn("Error closing server socket: {}", e.getMessage());
        }
    }

    private void closeQuietly(Socket socket){
        if (socket == null){
            return;
        }
        try{
            socket.close();
        } catch(IOException e){
            logger.warn("Error closing client socket: {}", e.getMessage());
        }
    }
}
