package com.ygtech.java_fullstack_journey.java_basics.day04;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class DeadlockDemo {
    static final Object lockA = new Object();
    static final Object lockB = new Object();

    static void work(Object first, Object second, String name) {
        synchronized (first) {
            System.out.println(name + " holds its first lock");
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            synchronized (second) {
                System.out.println(name + " got both locks");
            }
        }
    }

    static void run(boolean fixed) throws InterruptedException {
        Thread t1 = new Thread(() -> work(lockA, lockB, "T1"));
        // Deadlock version: T2 takes locks in the OPPOSITE order. Fixed version: same order.
        Thread t2 = new Thread(() -> {
            if (fixed) work(lockA, lockB, "T2");
            else work(lockB, lockA, "T2");
        });
        t1.setDaemon(true);   // daemon threads let the JVM exit even if they're stuck
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(2000);
        t2.join(2000);

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = bean.findDeadlockedThreads();
        System.out.println(fixed ? "FIXED" : "BROKEN");
        System.out.println("  t1 alive: " + t1.isAlive() + ", t2 alive: " + t2.isAlive());
        System.out.println("  deadlocked threads: " + (ids == null ? 0 : ids.length));
    }

    public static void main(String[] args) throws InterruptedException {
        run(false);   // deadlocks
        run(true);    // consistent lock order, no deadlock
    }
}