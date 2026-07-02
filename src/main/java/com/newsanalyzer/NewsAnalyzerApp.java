package com.newsanalyzer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newsanalyzer.client.MockNewsFeed;
import com.newsanalyzer.server.NewsAnalyzerServer;

/**
 * Application entry point for running the analyzer server or mock client.
 */
public final class NewsAnalyzerApp{
    private static final Logger logger = LoggerFactory.getLogger(NewsAnalyzerApp.class);

    private NewsAnalyzerApp(){
    }

    public static void main(String[] args){
        if (isClientMode(args)){
            MockNewsFeed.main(new String[0]);
            return;
        }

        if (hasUnknownArguments(args)){
            printUsage();
            System.exit(1);
        }
        startServer();
    }

    private static boolean isClientMode(String[] args){
        return args.length == 1 && ("--client".equals(args[0]) || "-c".equals(args[0]));
    }

    private static boolean hasUnknownArguments(String[] args){
        return args.length > 0;
    }

    private static void startServer(){
        NewsAnalyzerServer server = new NewsAnalyzerServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, stopping server...");
            server.stop();
        }));

        try{
            server.start();
        } catch(IOException e){
            logger.error("Failed to start server: {}", e.getMessage());
            server.stop();
            System.exit(1);
        }
    }
    private static void printUsage(){
        System.out.println("Usage:");
        System.out.println("  java -jar news-analyzer.jar");
        System.out.println("  java -jar news-analyzer.jar --client");
        System.out.println("  java -jar news-analyzer.jar -c");
    }
}