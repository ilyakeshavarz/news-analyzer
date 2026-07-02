package com.newsanalyzer;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.newsanalyzer.core.NewsItem;
import com.newsanalyzer.core.PositiveNewsFilter;
import com.newsanalyzer.server.NewsAnalyzerServer;
import com.newsanalyzer.server.SummaryReporter;
import com.newsanalyzer.server.SummaryResult;

/**
 * End-to-end tests for the TCP news analyzer pipeline.
 */
class NewsAnalyzerIntegrationTest{

    @Test
    void filterShouldWorkWithRealData(){
        PositiveNewsFilter filter = new PositiveNewsFilter();

        String[] headlines = {
            "good up rise",
            "bad down fall",
            "success high good",
            "failure low bad"
        };

        boolean[] expected = {true, false, true, false};

        for (int i = 0; i < headlines.length; i++){
            assertEquals(expected[i], filter.isPositive(headlines[i]), "Unexpected filter result for headline: " + headlines[i]);
        }
    }

    @Test
    void shouldHandleFilterBoundaryCases(){
        PositiveNewsFilter filter = new PositiveNewsFilter();

        assertFalse(filter.isPositive("good bad"), "50% positive must not be treated as positive");
        assertTrue(filter.isPositive("good good bad"), "More than 50% positive should be true");
        assertTrue(filter.isPositive("GOOD RISE UP"));
    }

    @Test
    void shouldReportTopThreeUniqueHeadlinesFromMultipleClients() throws Exception{
        CapturingReporter reporter = new CapturingReporter();
        NewsAnalyzerServer server = new NewsAnalyzerServer(0, 3, reporter);
        Thread serverThread = new Thread(() -> {
            try{
                server.start();
            } catch(Exception ignored){
            }
        }, "integration-server");
        serverThread.start();

        assertTrue(server.awaitStarted(3, TimeUnit.SECONDS), "server did not start in time");
        int port = server.getPort();

        try (Socket clientOne = new Socket("localhost", port);
             Socket clientTwo = new Socket("localhost", port);
             BufferedWriter writerOne = new BufferedWriter(new OutputStreamWriter(clientOne.getOutputStream(), StandardCharsets.UTF_8));
             BufferedWriter writerTwo = new BufferedWriter(new OutputStreamWriter(clientTwo.getOutputStream(), StandardCharsets.UTF_8))){

            writeLine(writerOne, "{\"headline\":\"good rise success\",\"priority\":9}");
            writeLine(writerOne, "{\"headline\":\"bad down failure\",\"priority\":9}");
            writeLine(writerTwo, "{\"headline\":\"up good high\",\"priority\":7}");
            writeLine(writerTwo, "{\"headline\":\"good rise success\",\"priority\":5}");
            writeLine(writerOne, "{\"headline\":\"up rise high\",\"priority\":8}");
            writeLine(writerTwo, "{\"headline\":\"up good high\",\"priority\":99}");

            SummaryResult result = reporter.pollUntilPositiveCountAtLeast(4, 4, TimeUnit.SECONDS);
            assertNotNull(result, "summary was not generated");
            assertEquals(4, result.getPositiveCount());

            List<NewsItem> topItems = result.getTopItems();
            assertEquals(3, topItems.size());
            assertEquals("good rise success", topItems.get(0).getHeadline());
            assertEquals(9, topItems.get(0).getPriority());
            assertEquals("up rise high", topItems.get(1).getHeadline());
            assertEquals("up good high", topItems.get(2).getHeadline());
        } finally{
            server.stop();
            serverThread.join(3000);
        }
    }

    private void writeLine(BufferedWriter writer, String value) throws Exception{
        writer.write(value);
        writer.newLine();
        writer.flush();
    }

    private static final class CapturingReporter implements SummaryReporter{
        private final LinkedBlockingQueue<SummaryResult> results = new LinkedBlockingQueue<>();

        @Override
        public void report(SummaryResult result, int summaryIntervalSeconds, long timestampMillis){
            results.offer(result);
        }

        SummaryResult pollUntilPositiveCountAtLeast(int expectedCount, long timeout, TimeUnit unit) throws InterruptedException{
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline){
                long remainingNanos = deadline - System.nanoTime();
                SummaryResult result = results.poll(Math.max(1L, remainingNanos), TimeUnit.NANOSECONDS);
                if (result == null){
                    return null;
                }
                if (result.getPositiveCount() >= expectedCount){
                    return result;
                }
            }
            return null;
        }
    }
}
