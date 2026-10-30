package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;

public record ReturnedLoan(Loan loan, LocalDate returnedOn, int fine) {}