package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LibraryDemo {

    static List<String> titles(Collection<Book> books) {
        return books.stream().map(Book::title).toList();
    }

    static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + " -> OK");
        } catch (LibraryException e) {
            System.out.println(label + " -> " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Library lib = SeedData.create();
        LocalDate d0 = LocalDate.of(2026, 10, 1);

        System.out.println("Genres: " + lib.countByGenre());
        System.out.println("Search 'the': " + titles(lib.search("the")));

        lib.borrow("B101", 1, d0);
        lib.borrow("B102", 1, d0);
        lib.borrow("B103", 1, d0);
        attempt("Asha borrows a 4th book", () -> lib.borrow("B104", 1, d0));
        attempt("Ravi borrows a taken book", () -> lib.borrow("B101", 2, d0));
        attempt("Ravi borrows unknown book", () -> lib.borrow("B999", 2, d0));
        lib.borrow("B104", 2, d0);
        lib.borrow("B106", 2, d0.plusDays(4));

        System.out.println("Available: " + titles(lib.availableBooks()));

        LocalDate d1 = LocalDate.of(2026, 10, 20);
        System.out.println("Overdue on " + d1 + ":");
        for (Loan l : lib.overdueLoans(d1)) {
            System.out.println("  " + lib.getBook(l.isbn()).title()
                    + " (member " + l.memberId() + ", " + l.daysOverdue(d1) + " day(s) late)");
        }

        System.out.println("Fine for B101: " + lib.returnBook("B101", d1));
        System.out.println("Fine for B104: " + lib.returnBook("B104", d1));
        System.out.println("Total fines: " + lib.totalFinesCollected());

        lib.borrow("B101", 2, d1);   // free again, so Ravi can take it
        System.out.println("Ravi's loans: " + lib.loansOf(2).stream()
                .map(l -> lib.getBook(l.isbn()).title() + " due " + l.dueOn())
                .toList());

        System.out.println("Most borrowed:");
        for (Map.Entry<String, Long> e : lib.mostBorrowed(3)) {
            System.out.println("  " + lib.getBook(e.getKey()).title() + " x" + e.getValue());
        }
    }
}