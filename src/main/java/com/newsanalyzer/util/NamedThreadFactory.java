package com.newsanalyzer.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that creates named threads for clearer logging and debugging.
 */
public class NamedThreadFactory implements ThreadFactory{
    private final String name;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedThreadFactory(String name){
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r){
        return new Thread(r, name + "-" + counter.getAndIncrement());
    }
}