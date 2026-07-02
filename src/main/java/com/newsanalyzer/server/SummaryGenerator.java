package com.newsanalyzer.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newsanalyzer.core.NewsItem;

/**
 * Generates time-windowed summaries from positive news items.
 */
public final class SummaryGenerator{
    private static final Logger logger = LoggerFactory.getLogger(SummaryGenerator.class);
    private static final int TOP_HEADLINE_LIMIT = 3;

    private final BlockingQueue<NewsItem> queue;
    private final long windowMillis;

    public SummaryGenerator(BlockingQueue<NewsItem> queue, int windowSeconds){
        if (queue == null){
            throw new IllegalArgumentException("queue must not be null");
        }
        if (windowSeconds <= 0){
            throw new IllegalArgumentException("windowSeconds must be greater than zero");
        }

        this.queue = queue;
        this.windowMillis = windowSeconds * 1000L;
    }

    public SummaryResult generate(long now){
        long cutoff = now - windowMillis;
        List<NewsItem> recent = new ArrayList<>();
        Iterator<NewsItem> iterator = queue.iterator();

        while (iterator.hasNext()){
            NewsItem item = iterator.next();

            if (item.getTimestamp() >= cutoff){
                recent.add(item);
            } else{
                iterator.remove();
                logger.debug("Removed expired item: {}", item.getHeadline());
            }
        }
        List<NewsItem> topItems = findTopUniqueHeadlines(recent);
        return new SummaryResult(recent.size(), topItems);
    }

    private List<NewsItem> findTopUniqueHeadlines(List<NewsItem> recent){
        Map<String, NewsItem> bestItemByHeadline = new HashMap<>();

        for (NewsItem item : recent){
            NewsItem currentBest = bestItemByHeadline.get(item.getHeadline());

            if (currentBest == null || compareByPriorityThenTimestamp(item, currentBest) < 0){
                bestItemByHeadline.put(item.getHeadline(), item);
            }
        }
        List<NewsItem> uniqueBestItems = new ArrayList<>(bestItemByHeadline.values());
        uniqueBestItems.sort(this::compareByPriorityThenTimestamp);

        if (uniqueBestItems.size() <= TOP_HEADLINE_LIMIT){
            return uniqueBestItems;
        }

        return new ArrayList<>(uniqueBestItems.subList(0, TOP_HEADLINE_LIMIT));
    }

    private int compareByPriorityThenTimestamp(NewsItem left, NewsItem right){
        int byPriority = Integer.compare(right.getPriority(), left.getPriority());
        if (byPriority != 0){
            return byPriority;
        }

        int byTimestamp = Long.compare(right.getTimestamp(), left.getTimestamp());
        if (byTimestamp != 0){
            return byTimestamp;
        }

        return left.getHeadline().compareTo(right.getHeadline());
    }
}