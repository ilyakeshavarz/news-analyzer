# News Analyzer

![Java CI](https://github.com/ilyakeshavarz/News-Analyzer/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-8-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/Build-Maven-blue?logo=apachemaven)
![JUnit 5](https://img.shields.io/badge/Tests-JUnit%205-brightgreen)
![License](https://img.shields.io/github/license/ilyakeshavarz/News-Analyzer)

A Java-based TCP news analyzer that receives news messages from multiple mock feed clients, validates incoming data, filters positive headlines, and periodically reports summary statistics.

## Overview

News Analyzer simulates a real-time news processing system. Mock clients generate random news headlines and send them to a central analyzer server over persistent TCP connections. The server validates each message, detects positive news using a configurable word-based strategy, and prints a summary every configured interval.

## Features

* Multi-client TCP server
* Persistent socket connections
* JSON-based message protocol
* Configurable mock news feed client
* Positive headline detection using Strategy Pattern
* Message validation before processing
* Time-windowed summary generation
* Top 3 unique positive headlines by priority
* Graceful shutdown support
* Reconnect logic with exponential backoff and jitter
* Console and file logging with Logback
* Unit and integration tests with JUnit 5

## Tech Stack

* Java 8
* Maven
* TCP Sockets
* Jackson
* SLF4J
* Logback
* JUnit 5

## Project Structure

```text
news-analyzer/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/newsanalyzer/
│   │   │       ├── NewsAnalyzerApp.java
│   │   │       ├── client/
│   │   │       │   └── MockNewsFeed.java
│   │   │       ├── config/
│   │   │       │   └── AppConfig.java
│   │   │       ├── core/
│   │   │       │   ├── NewsFilterStrategy.java
│   │   │       │   ├── PositiveNewsFilter.java
│   │   │       │   ├── NewsMessage.java
│   │   │       │   ├── NewsMessageValidator.java
│   │   │       │   ├── ValidationResult.java
│   │   │       │   └── NewsItem.java
│   │   │       ├── server/
│   │   │       │   ├── NewsAnalyzerServer.java
│   │   │       │   ├── SummaryGenerator.java
│   │   │       │   ├── SummarySnapshot.java
│   │   │       │   ├── SummaryReporter.java
│   │   │       │   └── ConsoleSummaryReporter.java
│   │   │       └── util/
│   │   │           └── NamedThreadFactory.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── com/newsanalyzer/
│               ├── NewsAnalyzerIntegrationTest.java
│               ├── config/
│               │   └── AppConfigTest.java
│               ├── core/
│               │   ├── NewsMessageValidatorTest.java
│               │   └── PositiveNewsFilterTest.java
│               └── server/
│                   └── SummaryGeneratorTest.java
├── pom.xml
└── README.md
```

## How It Works

1. `MockNewsFeed` generates random headlines and priorities.
2. Messages are serialized as JSON and sent over TCP.
3. `NewsAnalyzerServer` accepts multiple client connections.
4. Incoming messages are parsed and validated.
5. Positive headlines are detected using `PositiveNewsFilter`.
6. Accepted positive news items are stored in a queue.
7. `SummaryGenerator` periodically creates a summary.
8. `ConsoleSummaryReporter` displays the positive count and top headlines.

## Message Format

Each client sends one JSON message per line:

```json
{
  "headline": "good rise success",
  "priority": 8
}
```

Rules:

* `headline` must contain 3 to 5 words.
* Words must come from the allowed vocabulary.
* `priority` must be between 0 and 9.
* Higher priority values are less likely to be generated.

## Positive News Rule

A headline is considered positive only when more than 50% of its words are positive.

Positive words:

```text
up, rise, good, success, high
```

Example:

```text
good rise success  -> positive
bad down failure   -> negative
good bad           -> negative
```

## Running the Project

Build the project:

```bash
mvn clean package
```

Run the analyzer server:

```bash
java -jar target/news-analyzer-2.1.0.jar
```

Run a mock news feed client:

```bash
java -jar target/news-analyzer-2.1.0.jar --client
```

You can run multiple clients at the same time to test concurrent connections.

## Configuration

Main settings are defined in:

```text
src/main/resources/application.properties
```

Example options:

```properties
server.port=8888
summary.interval.seconds=10
feed.interval.millis=1000
queue.capacity=1000
positive.words=up,rise,good,success,high
news.allowed.words=up,down,rise,fall,good,bad,success,failure,high,low
```

Values can also be overridden using JVM system properties:

```bash
-Dserver.port=9999
-Dfeed.interval.millis=500
```

## Sample Output

```text
Positive count: 4
Top headlines:
   1. "good rise success" [p=9]
   2. "up rise high" [p=8]
   3. "up good high" [p=7]
```

## Testing

Run all tests:

```bash
mvn test
```

The test suite includes:

* Configuration tests
* Message validation tests
* Positive filter tests
* Summary generation tests
* End-to-end TCP integration tests

## Design Highlights

* Strategy Pattern for headline filtering
* Producer-consumer model using `BlockingQueue`
* Thread pool for handling multiple persistent clients
* Scheduled summary generation
* DTOs for TCP message exchange
* Immutable result objects for validation and summary output
* Testable design through constructor-based dependency injection
