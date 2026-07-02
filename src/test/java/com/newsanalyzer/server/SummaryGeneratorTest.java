package com.newsanalyzer.server;

import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.newsanalyzer.core.NewsItem;

/**
 * Unit tests for summary generation, expiration, ordering, and uniqueness.
 */
class SummaryGeneratorTest{

    @Test
    void shouldReturnTopPriorityItems(){
        long now = 10_000L;
        LinkedBlockingQueue<NewsItem> queue = new LinkedBlockingQueue<>();

        queue.add(new NewsItem("good rise high", 5, now));
        queue.add(new NewsItem("success up good", 8, now));
        queue.add(new NewsItem("rise high success", 3, now));

        SummaryGenerator generator = new SummaryGenerator(queue, 10);
        SummaryResult result = generator.generate(now);

        assertEquals(3, result.getPositiveCount());
        assertEquals(3, result.getTopItems().size());
        assertEquals("success up good", result.getTopItems().get(0).getHeadline());
        assertEquals(8, result.getTopItems().get(0).getPriority());
    }

    @Test
    void shouldRemoveExpiredItems(){
        long now = 10_000L;
        LinkedBlockingQueue<NewsItem> queue = new LinkedBlockingQueue<>();

        queue.add(new NewsItem("old good rise", 9, now - 2_000L));

        SummaryGenerator generator = new SummaryGenerator(queue, 1);
        SummaryResult result = generator.generate(now);

        assertEquals(0, result.getPositiveCount());
        assertTrue(result.getTopItems().isEmpty());
        assertTrue(queue.isEmpty());
    }

    @Test
    void shouldKeepRecentItemsInsideWindow(){
        long now = 10_000L;
        LinkedBlockingQueue<NewsItem> queue = new LinkedBlockingQueue<>();

        queue.add(new NewsItem("good rise high", 7, now - 500L));

        SummaryGenerator generator = new SummaryGenerator(queue, 1);
        SummaryResult result = generator.generate(now);

        assertEquals(1, result.getPositiveCount());
        assertEquals(1, result.getTopItems().size());
        assertEquals("good rise high", result.getTopItems().get(0).getHeadline());
    }

    @Test
    void shouldReturnUniqueHeadlinesButCountAllPositiveNews(){
        long now = 10_000L;
        LinkedBlockingQueue<NewsItem> queue = new LinkedBlockingQueue<>();

        queue.add(new NewsItem("good rise high", 8, now));
        queue.add(new NewsItem("good rise high", 7, now));
        queue.add(new NewsItem("good rise high", 6, now));
        queue.add(new NewsItem("success up high", 5, now));

        SummaryGenerator generator = new SummaryGenerator(queue, 10);
        SummaryResult result = generator.generate(now);

        assertEquals(4, result.getPositiveCount());
        assertEquals(2, result.getTopItems().size());
        assertEquals("good rise high", result.getTopItems().get(0).getHeadline());
        assertEquals("success up high", result.getTopItems().get(1).getHeadline());
    }

    @Test
    void shouldReturnOnlyThreeTopUniqueHeadlines(){
        long now = 10_000L;
        LinkedBlockingQueue<NewsItem> queue = new LinkedBlockingQueue<>();

        queue.add(new NewsItem("good rise high", 1, now));
        queue.add(new NewsItem("success up good", 9, now));
        queue.add(new NewsItem("rise high success", 7, now));
        queue.add(new NewsItem("up good high", 8, now));
        queue.add(new NewsItem("good success rise", 6, now));

        SummaryGenerator generator = new SummaryGenerator(queue, 10);
        SummaryResult result = generator.generate(now);

        assertEquals(5, result.getPositiveCount());
        assertEquals(3, result.getTopItems().size());
        assertEquals("success up good", result.getTopItems().get(0).getHeadline());
        assertEquals("up good high", result.getTopItems().get(1).getHeadline());
        assertEquals("rise high success", result.getTopItems().get(2).getHeadline());
    }

    @Test
    void shouldPreferNewestItemWhenPriorityIsEqual(){
        long now = 10_000L;
        LinkedBlockingQueue<NewsItem> queue = new LinkedBlockingQueue<>();

        queue.add(new NewsItem("good rise high", 7, now - 500L));
        queue.add(new NewsItem("success up good", 7, now));

        SummaryGenerator generator = new SummaryGenerator(queue, 10);
        SummaryResult result = generator.generate(now);

        assertEquals(2, result.getPositiveCount());
        assertEquals("success up good", result.getTopItems().get(0).getHeadline());
    }
}