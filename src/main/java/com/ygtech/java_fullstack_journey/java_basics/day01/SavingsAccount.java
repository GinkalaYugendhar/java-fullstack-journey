package com.ygtech.java_fullstack_journey.java_basics.day01;

public class SavingsAccount  extends Account{
    public SavingsAccount(String number , double opening, Notifier notifier) {
        super(number, opening, notifier);
    }
    @Override protected double minBalance() { return 1000; }
    @Override public String type() { return "Savings"; }
}
