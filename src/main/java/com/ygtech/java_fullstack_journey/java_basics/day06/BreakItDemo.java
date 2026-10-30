package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;
import java.util.*;

/**
 * Day 6 "Break it on purpose". Self-contained: you do NOT need to edit Library.java.
 * It uses nested copies of the "broken" versions next to the real, safe Library.
 */
public class BreakItDemo {

    // ---- Experiment 1 helper: a library that LEAKS its internal collection ----
    static class LeakyLibrary {
        private final Map<String, Book> books = new LinkedHashMap<>();
        void addBook(Book b) { books.put(b.isbn(), b); }
        Collection<Book> allBooks() { return books.values(); }   // BUG: hands out the real thing
    }

    public static void main(String[] args) {
        LocalDate d0 = LocalDate.of(2026, 10, 1);

        // ------------------------------------------------------------
        // 1. Leak the internals
        // ------------------------------------------------------------
        System.out.println("--- 1. Leaking internal collections ---");
        LeakyLibrary leaky = new LeakyLibrary();
        leaky.addBook(new Book("B1", "Effective Java", "Joshua Bloch", "Programming", 2018));
        leaky.addBook(new Book("B2", "Clean Code", "Robert C. Martin", "Programming", 2008));
        System.out.println("  Leaky library has " + leaky.allBooks().size() + " books");
        leaky.allBooks().clear();   // a caller wipes the library from outside!
        System.out.println("  After caller does allBooks().clear(): " + leaky.allBooks().size() + " books  <-- BAD");

        Library lib = SeedData.create();
        System.out.println("  Real library has " + lib.allBooks().size() + " books");
        try {
            lib.allBooks().clear();
        } catch (UnsupportedOperationException e) {
            System.out.println("  Real library: caller's clear() -> " + e.getClass().getSimpleName() + " (protected)");
        }
        System.out.println("  Real library still has " + lib.allBooks().size() + " books");

        // ------------------------------------------------------------
        // 2. Skip validation: compact constructor rejects bad data
        // ------------------------------------------------------------
        System.out.println("\n--- 2. Compact constructor validation ---");
        try {
            new Book("", "No ISBN", "x", "y", 2000);
        } catch (IllegalArgumentException e) {
            StackTraceElement top = e.getStackTrace()[0];
            System.out.println("  Caught: " + e.getMessage());
            System.out.println("  Thrown at: " + top.getClassName() + "." + top.getMethodName()
                    + " (" + top.getFileName() + ":" + top.getLineNumber() + ")");
            System.out.println("  -> the compact constructor in Book.java ran BEFORE the fields were assigned,");
            System.out.println("     so a Book with a blank ISBN can never exist.");
        }

        // ------------------------------------------------------------
        // 3. Modify while iterating (ConcurrentModificationException)
        // ------------------------------------------------------------
        System.out.println("\n--- 3. Removing from a map while looping over it ---");
        // Same shape as Library.activeLoans: book id -> its one loan
        Map<String, Loan> activeLoans = new HashMap<>();
        for (String isbn : List.of("B101", "B102", "B103")) {
            activeLoans.put(isbn, new Loan(isbn, 1, d0, d0.plusDays(14)));
        }
        try {
            for (Loan l : activeLoans.values()) {
                if (l.memberId() == 1) {
                    activeLoans.remove(l.isbn());   // removes from the map we're looping over
                }
            }
            System.out.println("  No exception this time (not guaranteed either way, never rely on it)");
        } catch (ConcurrentModificationException e) {
            System.out.println("  Caught: " + e);
        }

        // Fix A (uses the real Library): loop over a SNAPSHOT of the member's loans
        Library lib2 = SeedData.create();
        lib2.borrow("B101", 1, d0);
        lib2.borrow("B102", 1, d0);
        lib2.borrow("B103", 1, d0);
        for (Loan l : new ArrayList<>(lib2.loansOf(1))) {   // snapshot: a separate list
            lib2.returnBook(l.isbn(), d0);
        }
        System.out.println("  Fix A (snapshot): Asha now has " + lib2.loansOf(1).size() + " loans");

        // Fix B: removeIf on the live collection (it handles iteration safely)
        Map<String, Loan> copy = new HashMap<>();
        for (String isbn : List.of("B101", "B102", "B103")) {
            copy.put(isbn, new Loan(isbn, 1, d0, d0.plusDays(14)));
        }
        copy.values().removeIf(l -> l.memberId() == 1);
        System.out.println("  Fix B (removeIf): " + copy.size() + " loans left in the map");

        // ------------------------------------------------------------
        // 4. Unhandled NumberFormatException: read a stack trace top-down
        // ------------------------------------------------------------
        System.out.println("\n--- 4. Reading a stack trace ---");
        try {
            Integer.parseInt("abc");   // what Main's askInt does with the text "abc"
        } catch (NumberFormatException e) {
            System.out.println("  " + e);   // 1. exception type + message: WHAT happened
            StackTraceElement[] frames = e.getStackTrace();
            for (int i = 0; i < Math.min(4, frames.length); i++) {
                System.out.println("    at " + frames[i]);   // 2. top frame = where it was thrown
            }
            System.out.println("    ... (the LAST frame is where your program started: main)");
            System.out.println("  Read order: (1) exception + message, (2) first frame in YOUR code,");
            System.out.println("  (3) walk down to see who called it. Library code at the top, your code below.");
            System.out.println("  In Main.java: remove 'catch (NumberFormatException e)' and enter abc as a member id");
            System.out.println("  to see the real crash. Restore the catch afterwards.");
        }
    }
}