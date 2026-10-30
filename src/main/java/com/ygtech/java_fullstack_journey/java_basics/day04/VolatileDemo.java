package com.ygtech.java_fullstack_journey.java_basics.day04;

public class VolatileDemo {
    static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            long loops = 0;
            while (running) {
                loops++;
            }
            System.out.println("Worker stopped after " + loops + " loops");
        });
        worker.start();

        Thread.sleep(500);
        running = false;
        worker.join();
        System.out.println("Main finished");
    }
}