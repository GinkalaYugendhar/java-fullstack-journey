package com.ygtech.java_fullstack_journey.java_basics.day04;

public class ThreadBasics {

    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("Extends Thread -> " + getName());
        }
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            System.out.println("Runnable -> " + Thread.currentThread().getName());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new MyThread();
        Thread t2 = new Thread(new Task(), "worker-2");
        Thread t3 = new Thread(() -> System.out.println("Lambda -> " + Thread.currentThread().getName()), "worker-3");

        System.out.println("State before start: " + t2.getState());   // NEW
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
        System.out.println("State after finish: " + t2.getState());   // TERMINATED

        // start() vs run()
        Thread t4 = new Thread(() -> System.out.println("Ran on: " + Thread.currentThread().getName()), "worker-4");
        t4.run();     // runs on main: NO new thread
        t4 = new Thread(() -> System.out.println("Ran on: " + Thread.currentThread().getName()), "worker-4");
        t4.start();   // runs on worker-4
        t4.join();

        // sleep and interrupt
        Thread sleeper = new Thread(() -> {
            try {
                System.out.println("Sleeping for 10 seconds...");
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted, cleaning up");
                Thread.currentThread().interrupt();
            }
        });
        sleeper.start();
        Thread.sleep(300);
        sleeper.interrupt();
        sleeper.join();
        System.out.println("Done");
    }
}