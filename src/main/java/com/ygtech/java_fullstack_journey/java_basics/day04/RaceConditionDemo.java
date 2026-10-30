package com.ygtech.java_fullstack_journey.java_basics.day04;

import java.util.concurrent.atomic.AtomicInteger;

public class RaceConditionDemo {
    static int unsafeCount = 0;
    static int syncCount = 0;
    static final AtomicInteger atomicCount = new AtomicInteger();
    static final Object lock = new Object();

    static void runInThreads(Runnable task) throws InterruptedException {
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 4 threads x 100,000 increments = 400,000 expected

        runInThreads(() -> {
            for (int i = 0; i < 100_000; i++) unsafeCount++;
        });
        System.out.println("Unsafe:       " + unsafeCount);

        runInThreads(() -> {
            for (int i = 0; i < 100_000; i++) {
                synchronized (lock) { syncCount++; }
            }
        });
        System.out.println("Synchronized: " + syncCount);

        runInThreads(() -> {
            for (int i = 0; i < 100_000; i++) atomicCount.incrementAndGet();
        });
        System.out.println("Atomic:       " + atomicCount.get());
    }
}