# Day 4 Notes: Multithreading Basics

## Core concepts

- A **process** is a running program with its own memory. A **thread** is a path of execution inside a process. Threads in the same process **share heap memory**, which makes communication easy and bugs easy.
- **Concurrency**: multiple tasks making progress in overlapping time (even on one core, because the OS switches rapidly). **Parallelism**: tasks literally running at the same moment on different cores.
- Why bother: keep an app responsive while waiting (network, database, disk) and use multiple cores.
- Why it matters for Spring: every web request runs on a thread, and beans are shared by all of them.

## Creating threads

1. **Extend `Thread`**: works, but weakest (single inheritance, and it mixes the task with the mechanism).
2. **Implement `Runnable`**: better, because the task is separate from the thread.
3. **Lambda**: the modern form of option 2.

```java
Thread t = new Thread(() -> System.out.println("Hello from " + Thread.currentThread().getName()));
t.start();
```

### The two most common mistakes

- **`start()` vs `run()`**: `start()` creates a new thread that then calls `run()`. Calling `run()` yourself runs the code on the *current* thread, with no concurrency. (In my demo, `t4.run()` printed `Ran on: main`.)
- **A thread can be started only once.** A second `start()` throws `IllegalThreadStateException`.

### Useful methods

| Method | What it does |
|--------|--------------|
| `start()` | Begin running in a new thread |
| `join()` | Wait until that thread finishes |
| `Thread.sleep(ms)` | Pause the current thread (throws `InterruptedException`) |
| `interrupt()` | Politely ask a thread to stop |
| `setDaemon(true)` | Daemon threads don't keep the JVM alive |
| `Thread.currentThread()` | The thread running this code |

### Lifecycle

`NEW` → `RUNNABLE` → `BLOCKED` (waiting for a monitor lock) / `WAITING` (e.g. `join()`) / `TIMED_WAITING` (e.g. `sleep`) → `TERMINATED`. Read it with `thread.getState()`.

### Stopping a thread: interruption

You can't safely force-kill a thread (`stop()` is removed). Call `interrupt()` and let the thread's code cooperate:

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // restore the flag, then exit cleanly
    return;
}
```

**Rule:** never swallow an `InterruptedException` with an empty catch. Rethrow it, or restore the flag.

## Shared state: the hard part

### Race condition

`count++` looks like one step but is three: **read, add one, write back**. If two threads interleave, an update is lost (both read 5, both write 6, but the result should be 7). These bugs appear randomly and are hard to reproduce.

### Three tools

**1. `synchronized`**: only one thread at a time can enter the block for a given lock object. It gives:
- **Mutual exclusion** (no two threads inside at once)
- **Visibility** (changes made inside are visible to the next thread that takes the same lock)

```java
synchronized void increment() { count++; }     // locks on 'this'
static synchronized void inc() { ... }         // locks on the Class object
synchronized (lock) { ... }                    // locks on a specific object (preferred: small, private lock)
```

Keep blocks small and don't call unknown code (callbacks) while holding a lock. Locks are **reentrant**.

**2. `volatile`**: guarantees **visibility only**. Without it, one thread may never see another thread's write (CPU caching, compiler reordering). It does **not** make `count++` atomic. Use it for simple flags like `volatile boolean running`.

**3. Atomic classes** (`AtomicInteger`, `AtomicLong`, `AtomicReference`): lock-free thread-safe operations such as `incrementAndGet()`, built on hardware compare-and-swap. Best for simple counters.

| Need | Use |
|------|-----|
| A flag one thread sets and others read | `volatile` |
| A counter | `AtomicInteger` / `AtomicLong` |
| Several variables that must change together | `synchronized` (or a `Lock`) |

### Deadlock

Threads wait for each other forever: Thread 1 holds lock A and wants B, while Thread 2 holds B and wants A.

**Four conditions (all must hold):**
1. Mutual exclusion
2. Hold-and-wait
3. No preemption
4. Circular wait

**Simplest fix:** always acquire locks in the **same global order**. In my demo, the only difference between the broken and fixed versions was lock order.

### Why this matters for Spring

A Spring `@Service` or `@Controller` is a **singleton shared by all request threads**. Never store per-request data in a bean field, because two users' requests would overwrite each other. Keep beans stateless, or make shared state thread-safe. `HashMap` and `ArrayList` are not thread-safe (`ConcurrentHashMap` comes on Day 9).

## ExecutorService: thread pools

Creating a thread per task is expensive and unbounded. A **thread pool** reuses a fixed set of threads to run many tasks from a queue.

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> System.out.println("task running"));
pool.shutdown();
```

| Factory method | Behaviour | Use when |
|----------------|-----------|----------|
| `newFixedThreadPool(n)` | Exactly n threads, tasks queue up | Steady, bounded workload |
| `newCachedThreadPool()` | Creates threads as needed, reuses idle ones | Many short tasks (risky under heavy load) |
| `newSingleThreadExecutor()` | One thread, tasks run in order | Strict ordering |
| `newScheduledThreadPool(n)` | Delayed or periodic tasks | Timers, polling |
| `newVirtualThreadPerTaskExecutor()` | One cheap virtual thread per task (Java 21) | Many blocking I/O tasks |

**Sizing:** CPU-heavy work, about the number of cores (`Runtime.getRuntime().availableProcessors()`). I/O-heavy work, more threads or virtual threads.

**Virtual threads** (standard in Java 21) are lightweight threads managed by the JVM, great for I/O-bound apps. Spring Boot 3.2+ can use them via `spring.threads.virtual.enabled=true`. Learn the basics first, no need to master them yet.

### Runnable vs Callable, and Future

- `Runnable.run()` returns nothing and can't throw checked exceptions.
- `Callable<V>.call()` returns a value and can throw checked exceptions.
- `submit(callable)` returns a `Future<V>`: a handle to a result that may not be ready.
- `future.get()` **blocks** until the result is ready. If the task threw, `get()` throws an `ExecutionException` wrapping the real cause. `get(timeout, unit)` avoids waiting forever.

### Shutting down (always)

A fixed pool's threads are non-daemon, so if you forget to shut down, **the program never exits**.

- `shutdown()`: stop accepting new tasks, finish the queued ones
- `awaitTermination(time, unit)`: wait for them to finish
- `shutdownNow()`: try to interrupt running tasks and drop the queue
- Since Java 19, `ExecutorService` is `AutoCloseable`: `try (var pool = ...) { ... }`

**Production note:** the `Executors` factory methods hide unbounded queues or unbounded thread creation, which can exhaust memory. Real systems construct `ThreadPoolExecutor` (or Spring's `ThreadPoolTaskExecutor`) with explicit limits.

## What I built

In `java-basics/day04/`:

| File | What it shows |
|------|---------------|
| `ThreadBasics.java` | Three ways to create threads, `join`, states, `start()` vs `run()`, `interrupt()` |
| `RaceConditionDemo.java` | Unsafe counter vs `synchronized` vs `AtomicInteger` |
| `VolatileDemo.java` | A `volatile` flag stopping a worker thread |
| `DeadlockDemo.java` | A real deadlock, detection with `ThreadMXBean`, and the lock-ordering fix |
| `ExecutorDemo.java` | Fixed pool, `Callable`, `Future`, `ExecutionException`, timeout, clean shutdown |
| `ProducerConsumer.java` | `ArrayBlockingQueue` with a poison pill |

### What I observed

- **ThreadBasics:** the three `start()` lines print in any order. The sleeper wakes immediately on `interrupt()` instead of waiting 10 seconds.
- **RaceConditionDemo:** 4 threads x 100,000 increments should give 400000. Unsafe usually prints less, and a different number each run. `synchronized` and `AtomicInteger` always give 400000.
- **VolatileDemo:** with `volatile` it stops after about half a second. Without it, it may hang forever on some machines. Never rely on it working.
- **DeadlockDemo:** the broken version leaves both threads alive with 2 deadlocked threads. The fixed version finishes with 0.
- **ExecutorDemo:** 3 threads handle 6 tasks, so thread names repeat. Sum of squares is 91 (1+4+9+16+25+36). The failing task gives `java.lang.IllegalStateException: task failed` via `getCause()`.
- **ProducerConsumer:** the producer races ahead, then stalls when the queue holds 3 items and waits for the slow consumer. That is **backpressure**, and it is the main reason to use a bounded queue. With two consumers you need two poison pills, one per consumer.

## Things I broke on purpose

- Called `start()` twice: `IllegalThreadStateException`
- Removed `volatile` from the flag: possible infinite loop
- Commented out `shutdown()`: the program refused to exit
- Reversed lock order in one thread: deadlock

## DSA

**Longest Common Prefix (LeetCode 14)**
- Assume the first word is the prefix, then shrink it until every following word starts with it.
- Complexity: O(S) time, where S is the total characters across all strings. O(1) extra space.
- Edge cases: empty array, single word, empty string in the array, no common prefix (`["dog", "car"]`), identical words.

**Reverse Words in a String (LeetCode 151)**
- `trim()` removes outer spaces, `split("\\s+")` splits on runs of whitespace, then append from the end with a `StringBuilder`.
- Complexity: O(n) time, O(n) space.
- Why `StringBuilder`: strings are immutable, so repeated `+=` creates a new string every time.
- Edge cases: only spaces (gives `""`), a single word, leading and trailing spaces.
- Follow-up: O(1) extra space with a `char[]`. Reverse the whole array, then reverse each word back.

## Self-check answers

1. **What if you call `run()` instead of `start()`?** The code runs on the current thread with no concurrency.
2. **Why can two threads both read 5 and both write 6?** `count++` is read, add, write. If they interleave, both read 5 before either writes, so one update is lost.
3. **Does `volatile` make `count++` safe?** No. It gives visibility but not atomicity, so the three steps can still interleave.
4. **When use `synchronized` instead of `AtomicInteger`?** When several variables must change together, or the operation is more than a single atomic update.
5. **`Runnable` vs `Callable`?** `Callable` returns a value and can throw checked exceptions. `Runnable` does neither.
6. **What does `Future.get()` do if the task threw?** It throws `ExecutionException` wrapping the real cause.
7. **Why shouldn't a Spring singleton keep per-request data in a field?** The bean is shared by all request threads, so concurrent requests would overwrite each other's data.
8. **How do you prevent the A-then-B / B-then-A deadlock?** Always acquire locks in the same global order.

## One mistake I made today

> TODO: replace this with a real mistake from today (e.g. forgot `shutdown()` so the program never exited, swallowed an `InterruptedException`, used `run()` instead of `start()`, or forgot a second poison pill).