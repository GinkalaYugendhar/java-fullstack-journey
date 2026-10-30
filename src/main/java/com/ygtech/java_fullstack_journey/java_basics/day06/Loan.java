package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Loan(String isbn, int memberId, LocalDate borrowedOn, LocalDate dueOn) {

    public int daysOverdue(LocalDate today) {
        return (int) Math.max(0, ChronoUnit.DAYS.between(dueOn, today));
    }

    public boolean isOverdue(LocalDate today) {
        return daysOverdue(today) > 0;
    }
}