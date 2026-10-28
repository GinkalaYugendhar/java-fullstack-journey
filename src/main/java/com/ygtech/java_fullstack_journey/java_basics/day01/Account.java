package com.ygtech.java_fullstack_journey.java_basics.day01;

public abstract class Account {
    private final String accountNumber;     // private + final: cannot change
    private double balance;                 // private: only changed via methods
    private final Notifier notifier;

    protected Account(String accountNumber, double openingBalance, Notifier notifier) {
        this.accountNumber = accountNumber;
        this.balance = openingBalance;
        this.notifier = notifier;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposite must be positive");
        }
        balance += amount;
        notifier.send(accountNumber + ": deposited " + amount + ", balance " + balance);
    }

    public void withdraw(double amount) {
        if (amount <=0) {
            throw new IllegalArgumentException("Withdrawal must be positive");
        }
        if (balance - amount < minBalance()) {
            throw new IllegalStateException("Insufficient funds in " + accountNumber);
        }
        balance -= amount;
        notifier.send(accountNumber + ": withdraw " + amount + ", balance " + balance);
    }

    protected abstract double minBalance();    // each account type decides
    public  abstract String type();

    public double getBalance() { return balance; }
    public String getAccountNumber () { return accountNumber; }

}
