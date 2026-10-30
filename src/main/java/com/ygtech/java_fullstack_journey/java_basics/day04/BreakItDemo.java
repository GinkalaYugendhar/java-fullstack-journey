package com.ygtech.java_fullstack_journey.java_basics.day04;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Requires Java 21 (virtual threads in part 4)
public class BreakItDemo {

    static boolean plainFlag = true;             // NOT volatile
    static volatile boolean volatileFlag = true; // volatile

    public static void main(String[] args) throws Exception {

        // 1. BREAK IT: start() a thread twice -> IllegalThreadStateException
        System.out.println("--- 1. Starting a thread twice ---");
        Thread t = new Thread(() -> System.out.println("  running on " + Thread.currentThread().getName()), "worker-1");
        t.start();
        t.join();
        System.out.println("  state after finish: " + t.getState());
        try {
            t.start();   // a thread can be started only once
        } catch (IllegalThreadStateException e) {
            System.out.println("  Caught: " + e);
        }
        // Fix: create a new Thread object for each run

        // 2a. volatile flag: the worker sees the update and stops
        System.out.println("\n--- 2a. With volatile ---");
        Thread w1 = new Thread(() -> {
            long loops = 0;
            while (volatileFlag) loops++;
            System.out.println("  worker stopped after " + loops + " loops");
        });
        w1.setDaemon(true);
        w1.start();
        Thread.sleep(500);
        volatileFlag = false;
        w1.join(2000);
        System.out.println("  worker still alive? " + w1.isAlive() + "   (expected: false)");

        // 2b. BREAK IT: same thing without volatile. May hang forever on some machines.
        // The thread is a daemon and we only wait 2 seconds, so this demo can't freeze your run.
        System.out.println("\n--- 2b. Without volatile ---");
        Thread w2 = new Thread(() -> {
            long loops = 0;
            while (plainFlag) loops++;
            System.out.println("  worker stopped after " + loops + " loops");
        });
        w2.setDaemon(true);
        w2.start();
        Thread.sleep(500);
        plainFlag = false;
        w2.join(2000);
        if (w2.isAlive()) {
            System.out.println("  worker is STILL running: it never saw plainFlag = false (visibility bug)");
        } else {
            System.out.println("  worker stopped this time, but the Java Memory Model does NOT guarantee that.");
            System.out.println("  Never rely on it. Try a different machine/JVM or run it a few times.");
        }

        // 3. BREAK IT: forget to shut down a fixed thread pool
        System.out.println("\n--- 3. Forgetting pool.shutdown() ---");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> System.out.println("  task ran on " + Thread.currentThread().getName()
                + ", daemon? " + Thread.currentThread().isDaemon()));
        Thread.sleep(300);
        System.out.println("  isShutdown: " + pool.isShutdown());
        System.out.println("  Pool threads are NON-daemon and idle-waiting for work.");
        System.out.println("  If main ended here without shutdown(), the JVM would never exit.");
        // Fix: always shut down
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  after shutdown(), isTerminated: " + pool.isTerminated());

        // 4. Virtual threads vs fixed pool: compare thread names
        System.out.println("\n--- 4. Fixed pool vs virtual threads ---");
        try (ExecutorService fixed = Executors.newFixedThreadPool(3)) {
            for (int i = 1; i <= 6; i++) {
                final int n = i;
                fixed.submit(() -> System.out.println("  fixed   task " + n + " on "
                        + Thread.currentThread().getName()));
            }
        }   // try-with-resources (Java 19+) shuts down and waits
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 6; i++) {
                final int n = i;
                virtual.submit(() -> System.out.println("  virtual task " + n + " on \""
                        + Thread.currentThread().getName() + "\" isVirtual="
                        + Thread.currentThread().isVirtual()));
            }
        }
        System.out.println("  Fixed pool: names repeat (3 threads reused). Virtual: a new unnamed thread per task.");
    }
}