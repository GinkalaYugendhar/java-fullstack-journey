package com.ygtech.java_fullstack_journey.java_basics.day04;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ProducerConsumer {
    static final int POISON_PILL = -1;   // signals "no more items"

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);   // small capacity on purpose

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    queue.put(i);   // blocks when the queue is full
                    System.out.println("Produced " + i + " (queue size " + queue.size() + ")");
                }
                queue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    int item = queue.take();   // blocks when the queue is empty
                    if (item == POISON_PILL) break;
                    System.out.println("   Consumed " + item);
                    Thread.sleep(200);   // consumer is slower than the producer
                }
                System.out.println("Consumer finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }
}