# Concurrent Log Analyzer

A Java application for analyzing HTTP access logs and comparing sequential
and concurrent processing approaches.

The project was created to practice Java concurrency concepts such as
thread pools, blocking queues, synchronization, shared mutable state,
and producer-consumer architecture.

## Log format

Each log entry has the following format:

```text
timestamp method path statusCode responseTimeMs
```

Example:

```text
2026-10-10T10:15:31 GET /users 200 53
```

## Collected statistics

The analyzer calculates:

- total number of requests;
- number of server errors (`statusCode >= 500`);
- number of requests for each HTTP status;
- number of requests for each path;
- average response time;
- max response time.

## Base implementation

Processes the log file in a single thread:

```text
read line
   ↓
parse
   ↓
update statistics
```

This implementation is also used as a simple baseline for performance
comparison.

## Testing

The project contains tests for:

- log parsing;
- statistics calculation;
- concurrent processing.

The concurrent analyzer is tested on a generated file containing many
log entries to verify that no requests are lost when several workers
process logs simultaneously.

Synchronization can be removed from `Statistics.update()` and it clearly demonstrates a race
condition, because multiple workers may overwrite each other's updates and
produce incorrect results.

## Experiments

Large log files are generated automatically using `LogGenerator`.

Each implementation is executed several times:

- several warm-up runs are ignored;
- multiple measured runs are performed;
- the average execution time is calculated using `System.nanoTime()`.

The measurements currently include thread-pool creation and shutdown.

## First performance experiment

### Concurrent analyzer

The concurrent implementation uses a producer-consumer approach.

```text
                BlockingQueue<String>
                       │
Producer               │
reads file ───────────► │
                       │
              ┌────────┼────────┐
              ▼        ▼        ▼
           Worker 1 Worker 2 Worker N
              │        │        │
             parse    parse    parse
              │        │        │
              └──── shared Statistics ────┘
```

The main thread reads lines from the file and puts them into a bounded
`BlockingQueue`.

Worker tasks are executed by a fixed thread pool. Each worker repeatedly:

1. takes a log line from the queue;
2. parses it into a `LogEntry`;
3. updates shared statistics.

After the whole file has been read, the producer adds one stop-flag
for each worker so that all workers can terminate.

## Synchronization

`Statistics` is shared between multiple worker threads.

The first concurrent implementation protects the whole update operation
with `synchronized`:

```java
public synchronized void update(LogEntry log) {
    ...
}
```

This one compares:

- sequential processing;
- concurrent processing with 2 workers;
- concurrent processing with 4 workers;
- concurrent processing with 8 workers.

## Benchmark results

The benchmark was run on a generated log file containing 1,000,000 log.

| Implementation | Workers | Average time |
|---|---:|-------------:|
| Sequential | 1 |       606,03 |
| Synchronized concurrent | 2 |       472,17 |
| Synchronized concurrent | 4 |       420,52 |
| Synchronized concurrent | 8 |       495,30 |

The 4-worker configuration produced the best result in this experiment.

Using 8 workers was slower than using 4 workers. The current implementation
synchronizes the entire statistics update operation, so only one worker can
modify the statistics at a time. With more workers, lock contention, queue
coordination and thread scheduling overhead start to outweigh the benefits
of additional parallelism.
