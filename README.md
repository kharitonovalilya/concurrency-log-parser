# Concurrent Log Analyzer

A Java application for analyzing HTTP access logs and provide some experiments of compares different methods to reach thread-safe concurrency.

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

## Experiments

Large log files are generated automatically using `LogGenerator`.

Each implementation is executed several times:

- several warm-up runs are ignored;
- multiple measured runs are performed;
- the average execution time is calculated using `System.nanoTime()`.

The measurements currently include thread-pool creation and shutdown.

## Experiment 1: compare single-thread and concurrent

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
with `synchronized`.

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

## Experiment 2: synchronized vs concurrent data structures

The second implementation replaces:

- `LongAdder` for request counters and total response time;
- `AtomicLong` for maximum response time;
- `ConcurrentHashMap<K, LongAdder>` for grouped counters.

## Benchmark results

| Implementation | Workers | Average time |
|---|---:|---:|
| Sequential | 1 | 643.31 ms |
| Synchronized | 1 | 486.78 ms |
| Synchronized | 2 | 496.73 ms |
| Synchronized | 4 | 396.48 ms |
| Synchronized | 8 | 478.49 ms |
| Concurrent structures | 1 | 512.87 ms |
| Concurrent structures | 2 | 509.69 ms |
| Concurrent structures | 4 | 402.81 ms |
| Concurrent structures | 8 | 470.81 ms |

The best result in both concurrent implementations was achieved with
4 worker threads, as in first.

Consequently, additional threads introduce scheduling, queue coordination and contention
over shared state, so increasing parallelism does not necessarily improve
performance.

Replacing the single synchronized update with concurrent
data structures did not produce a significant improvement.
For example, with 4 workers the synchronized version took 396.48 ms,
whereas the concurrent-structures version took 402.81 ms.

This suggests that statistics synchronization is not the main bottleneck
of the application. File reading, parsing, `BlockingQueue` coordination
and other parts of the processing pipeline also contribute significantly
to the total execution time.
