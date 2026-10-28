package com.ygtech.java_fullstack_journey.java_basics.day01;

import java.util.List;

public class Main {
    public static void main(String[] args) {
//        Notifier notifier = new EmailNotifier();

        Notifier notifier = new SmsNotifier();
        // One list, different object types: polymorphism
        List<Account> accounts = List.of(
                new SavingsAccount("SAV-001", 5000, notifier),
                new CurrentAccount("CUR-001", 2000, notifier)
        );

        for (Account acc : accounts) {
            System.out.println("Type: " + acc.type());
            try {
                acc.withdraw(4500);
            } catch (IllegalStateException e) {
                System.out.println("Caught: " + e.getMessage());
            }
        }
    }
}
