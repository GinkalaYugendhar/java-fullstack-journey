package com.ygtech.java_fullstack_journey.java_basics.day01;

public class CurrentAccount extends Account {
    public CurrentAccount(String number, double opening, Notifier notifier) {
        super(number, opening, notifier);
    }
    @Override protected double minBalance() { return -5000; }
    @Override public String type() { return "Current"; }
}
