package com.ygtech.java_fullstack_journey.java_basics.day04;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExecutorDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                final int n = i;
                futures.add(pool.submit(() -> {
                    Thread.sleep(300);   // simulate slow work (Callable can throw checked exceptions)
                    System.out.println(Thread.currentThread().getName() + " squared " + n);
                    return n * n;
                }));
            }

            int total = 0;
            for (Future<Integer> f : futures) {
                total += f.get();   // blocks until this result is ready
            }
            System.out.println("Sum of squares: " + total);   // 91

            // A task that fails: the exception arrives wrapped in ExecutionException
            Future<Integer> bad = pool.submit(() -> {
                if (true) throw new IllegalStateException("task failed");
                return 0;
            });
            try {
                bad.get();
            } catch (ExecutionException e) {
                System.out.println("Cause: " + e.getCause());
            }

            // A timeout
            Future<String> slow = pool.submit(() -> {
                Thread.sleep(3000);
                return "finally";
            });
            try {
                slow.get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("Gave up waiting");
                slow.cancel(true);
            }
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
        System.out.println("Pool shut down");
    }
}