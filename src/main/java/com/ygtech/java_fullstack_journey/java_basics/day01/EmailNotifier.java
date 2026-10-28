package com.ygtech.java_fullstack_journey.java_basics.day01;

public class EmailNotifier implements Notifier{
    @Override
    public void send(String message) {
        System.out.println("[Email] " + message);
    }
}
