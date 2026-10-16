# Concurrent Log Analyzer

A Java application for analyzing HTTP access logs and experimenting with
different approaches to thread-safe concurrent processing.

The project was created to practice Java concurrency concepts such as
thread pools, blocking queues, synchronization, shared mutable state,
and producer-consumer architecture.

## Contents

- [Log format](#log-format)
- [Collected statistics](#collected-statistics)
- [Sequential implementation](#sequential-implementation)
- [Testing](#testing)
- [Failure handling](#failure-handling)
- [Experiments](#experiments)
    - [Experiment 1: single-thread vs concurrent processing](#experiment-1-single-thread-vs-concurrent-processing)
    - [Experiment 2: synchronized vs concurrent data structures](#experiment-2-synchronized-vs-concurrent-data-structures)
    - [Experiment 3: isolated statistics contention](#experiment-3-isolated-statistics-contention)
- [Key findings](#key-findings)
- [Limitations](#limitations)

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

## Sequential implementation

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

## Failure handling

The concurrent analyzer follows a fail-fast strategy.

If a worker fails while parsing or processing a log entry, the failure is
stored and the producer thread is interrupted. The remaining worker tasks
are then cancelled through executor shutdown.

This prevents the producer from blocking indefinitely on a full bounded
queue when one or more consumers have already failed.

Normal completion and failure use different shutdown strategies:

- successful processing finishes workers using explicit stop signals and
  performs a graceful executor shutdown;
- failed or externally interrupted processing uses `shutdownNow()` to
  interrupt remaining worker tasks.

Queue control messages are represented separately from log contents, so
a log line cannot accidentally be interpreted as a worker stop signal.

# Experiments

Large log files are generated automatically using `LogGenerator`.

Each implementation is executed several times:

- several warm-up runs are ignored;
- multiple measured runs are performed;
- the average execution time is calculated using `System.nanoTime()`.

The end-to-end benchmarks include the complete analyzer lifecycle,
including thread-pool creation and shutdown.

The isolated statistics benchmark starts measurement only after all worker
threads are ready, in order to focus more closely on contention during
statistics updates.
## Experiment 1: compare single-thread and concurrent

### Concurrent analyzer

The concurrent implementation uses a producer-consumer approach.

```text
                BlockingQueue<QueueItem>
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

After the whole file has been read, the producer adds one stop signal
for each worker so that all workers can terminate gracefully.

## Synchronization

`Statistics` is shared between multiple worker threads.

The first concurrent implementation protects the whole update operation
with `synchronized`.

## Benchmark results

The benchmark was run on a generated log file containing 1,000,000 log entries.

| Implementation | Workers | Average time |
|---|---:|-------------:|
| Sequential | 1 |    606.03 ms |
| Synchronized concurrent | 2 |    472.17 ms |
| Synchronized concurrent | 4 |    420.52 ms |
| Synchronized concurrent | 8 |    495.30 ms |

The 4-worker configuration produced the best result in this experiment.

Using 8 workers was slower than using 4 workers. The current implementation
synchronizes the entire statistics update operation, so only one worker can
modify the statistics at a time. With more workers, lock contention, queue
coordination and thread scheduling overhead start to outweigh the benefits
of additional parallelism.

## Experiment 2: synchronized vs concurrent data structures

The second implementation replaces the single coarse-grained lock with:

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
4 worker threads, which is consistent with the first experiment.

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

## Experiment 3: isolated statistics contention

The end-to-end benchmark showed almost no performance difference between
the synchronized statistics implementation and the implementation based on
concurrent data structures. This suggested that statistics updates might not
be the main bottleneck of the complete log-processing pipeline.

To isolate the synchronization strategy itself, a separate benchmark was
created. One million `LogEntry` objects were generated before the measurement,
so file I/O, string parsing and `BlockingQueue` coordination were excluded.

`CountDownLatch` was used to make sure that all worker threads had already
started before the measurement began. The workers waited on a common start
latch and were released only after the timer was started.

| Implementation | Workers | Average time |
|---|---:|---:|
| Sequential | 1 | 23.12 ms |
| Synchronized | 1 | 21.32 ms |
| Synchronized | 2 | 46.89 ms |
| Synchronized | 4 | 100.18 ms |
| Synchronized | 8 | 126.40 ms |
| Concurrent structures | 1 | 12.50 ms |
| Concurrent structures | 2 | 20.57 ms |
| Concurrent structures | 4 | 32.84 ms |
| Concurrent structures | 8 | 63.45 ms |

The isolated benchmark shows a much clearer difference between the two
thread-safe implementations.

With `SynchronizedStatistics`, the whole `update()` operation is protected
by one monitor. Therefore, even when several worker threads are available,
only one of them can update the shared statistics at a time. As the number
of competing threads increases, lock contention grows and performance
degrades significantly.

`ConcurrentStatistics` uses `LongAdder`, `AtomicLong` and
`ConcurrentHashMap` instead of one coarse-grained lock. The workers still
access shared state and synchronization overhead does not disappear, but
they are no longer forced to serialize the entire update operation. This
allows the implementation to handle contention considerably better.

Optimizing shared statistics does improve the statistics subsystem itself, but this improvement
has only a small effect on the complete application because file reading,
parsing, queue coordination and other stages also contribute to the total
processing time.

# Key findings

The experiments demonstrated that adding more threads does not automatically
make an application faster.

In the end-to-end log-processing benchmark, performance improved as the
number of workers increased up to four threads. Increasing the worker count
further made processing slower because coordination and scheduling.

Replacing one `synchronized` statistics update with
`LongAdder`, `AtomicLong` and `ConcurrentHashMap` did not significantly
improve the performance of the complete processing pipeline.

However, after isolating statistics updates from file I/O, parsing and queue
coordination, the concurrent data-structure implementation handled contention
considerably better than the synchronized implementation.

This demonstrates that optimizing one concurrent subsystem does not necessarily improve the overall application
if that subsystem is not the main bottleneck.

The project also showed the importance of correct failure handling in
producer-consumer systems. Worker failures must be propagated to the producer
and remaining tasks must be cancelled to avoid blocked threads and unfinished
processing.