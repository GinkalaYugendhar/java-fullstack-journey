package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * Day 6 extensions: waitlist, renew, composable search, report export, and the money question.
 * Uses LibraryPlus (a copy of Library with the waitlist and renew additions).
 */
public class ExtensionsDemo {

    static LibraryPlus seed() {
        LibraryPlus lib = new LibraryPlus();
        lib.addBook(new Book("B101", "Effective Java",  "Joshua Bloch",      "Programming", 2018));
        lib.addBook(new Book("B102", "Clean Code",      "Robert C. Martin",  "Programming", 2008));
        lib.addBook(new Book("B103", "Design Patterns", "Erich Gamma",       "Programming", 1994));
        lib.addBook(new Book("B104", "The Alchemist",   "Paulo Coelho",      "Fiction",     1988));
        lib.addBook(new Book("B105", "Brave New World", "Aldous Huxley",     "Fiction",     1932));
        lib.addBook(new Book("B106", "Sapiens",         "Yuval Noah Harari", "History",     2011));
        lib.addBook(new Book("B107", "The Hobbit",      "J.R.R. Tolkien",    "Fiction",     1937));
        lib.addMember(new Member(1, "Asha"));
        lib.addMember(new Member(2, "Ravi"));
        lib.addMember(new Member(3, "Meera"));
        return lib;
    }

    static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  " + label + " -> OK");
        } catch (LibraryException e) {
            System.out.println("  " + label + " -> " + e.getMessage());
        }
    }

    static List<String> titles(List<Book> books) {
        return books.stream().map(Book::title).toList();
    }

    public static void main(String[] args) {
        LocalDate d0 = LocalDate.of(2026, 10, 1);

        waitlistDemo(d0);
        renewDemo(d0);
        filterDemo(d0);
        reportDemo(d0);
        moneyDemo();
    }

    // ------------------------------------------------------------
    // Extension 1: Waitlist
    // ------------------------------------------------------------
    static void waitlistDemo(LocalDate d0) {
        System.out.println("=== 1. Waitlist ===");
        LibraryPlus lib = seed();

        lib.borrow("B107", 1, d0);   // Asha has The Hobbit
        attempt("Ravi reserves The Hobbit", () -> lib.reserve("B107", 2));
        attempt("Meera reserves The Hobbit", () -> lib.reserve("B107", 3));
        attempt("Ravi reserves it again", () -> lib.reserve("B107", 2));
        attempt("Asha reserves her own book", () -> lib.reserve("B107", 1));
        attempt("Ravi reserves an available book", () -> lib.reserve("B101", 2));
        System.out.println("  Waitlist: " + lib.waitlistFor("B107"));

        LocalDate d3 = d0.plusDays(3);
        System.out.println("  Asha returns it, fine " + lib.returnBook("B107", d3));
        System.out.println("  Next in line: member " + lib.nextInWaitlist("B107").orElse(-1));
        attempt("Meera tries to jump the queue", () -> lib.borrow("B107", 3, d3));
        attempt("Ravi borrows (he is first)", () -> lib.borrow("B107", 2, d3));
        System.out.println("  Next in line now: member " + lib.nextInWaitlist("B107").orElse(-1));
        System.out.println();
    }

    // ------------------------------------------------------------
    // Extension 2: Renew
    // ------------------------------------------------------------
    static void renewDemo(LocalDate d0) {
        System.out.println("=== 2. Renew (new Loan record replaces the old one) ===");
        LibraryPlus lib = seed();

        Loan original = lib.borrow("B101", 1, d0);
        System.out.println("  Borrowed, due " + original.dueOn());
        Loan renewed = lib.renew("B101", d0.plusDays(5));
        System.out.println("  Renewed, due " + renewed.dueOn() + " (original record unchanged: " + original.dueOn() + ")");
        attempt("Second renewal", () -> lib.renew("B101", d0.plusDays(6)));

        lib.borrow("B102", 1, d0);
        lib.reserve("B102", 2);
        attempt("Renew while someone is waiting", () -> lib.renew("B102", d0.plusDays(2)));

        lib.borrow("B103", 1, d0);
        attempt("Renew an overdue book (day 20)", () -> lib.renew("B103", d0.plusDays(20)));
        attempt("Renew a book that is not on loan", () -> lib.renew("B105", d0));
        System.out.println();
    }

    // ------------------------------------------------------------
    // Extension 3: Composable search with Predicate.and()
    // ------------------------------------------------------------
    static void filterDemo(LocalDate d0) {
        System.out.println("=== 3. Composable search ===");
        LibraryPlus lib = seed();
        lib.borrow("B104", 1, d0);   // The Alchemist is on loan

        Predicate<Book> fiction = BookFilter.of(lib, "Fiction", null, null, false);
        Predicate<Book> oldFiction = BookFilter.of(lib, "Fiction", null, 1950, false);
        Predicate<Book> freeFiction = BookFilter.of(lib, "fiction", null, null, true);
        Predicate<Book> modernProgramming = BookFilter.of(lib, "Programming", 2000, 2020, true);
        Predicate<Book> fictionStartingWithThe = fiction.and(b -> b.title().startsWith("The"));   // extend any predicate

        System.out.println("  Fiction:                    " + titles(lib.allBooks().stream().filter(fiction).toList()));
        System.out.println("  Fiction up to 1950:         " + titles(lib.allBooks().stream().filter(oldFiction).toList()));
        System.out.println("  Fiction, available only:    " + titles(lib.allBooks().stream().filter(freeFiction).toList()));
        System.out.println("  Programming 2000-2020, free:" + " " + titles(lib.allBooks().stream().filter(modernProgramming).toList()));
        System.out.println("  Fiction starting with 'The':" + " " + titles(lib.allBooks().stream().filter(fictionStartingWithThe).toList()));
        System.out.println();
    }

    // ------------------------------------------------------------
    // Extension 4: Export the overdue report with try-with-resources
    // ------------------------------------------------------------
    static void reportDemo(LocalDate d0) {
        System.out.println("=== 4. Export report ===");
        LibraryPlus lib = seed();
        lib.borrow("B101", 1, d0);
        lib.borrow("B102", 1, d0);
        lib.borrow("B104", 2, d0);
        LocalDate d1 = d0.plusDays(20);   // 6 days past the 15 Oct due date

        Path file = Path.of("overdue.txt");
        try {
            int count = ReportExporter.writeOverdueReport(lib, d1, file);
            System.out.println("  Wrote " + count + " overdue loan(s) to " + file.toAbsolutePath());
            for (String line : Files.readAllLines(file)) {
                System.out.println("    " + line);
            }
        } catch (IOException e) {
            System.out.println("  Could not write report: " + e.getMessage());
        }

        // Handling IOException: writing into a folder that does not exist
        try {
            ReportExporter.writeOverdueReport(lib, d1, Path.of("no-such-folder", "overdue.txt"));
        } catch (IOException e) {
            System.out.println("  Expected failure -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println();
    }

    // ------------------------------------------------------------
    // Extension 5: why money shouldn't be a double (or even a plain int)
    // ------------------------------------------------------------
    static void moneyDemo() {
        System.out.println("=== 5. Money ===");
        System.out.println("  double: 0.1 + 0.2 = " + (0.1 + 0.2) + "  (binary floating point can't store 0.1 exactly)");
        System.out.println("  double: 0.1 + 0.2 == 0.3 ? " + (0.1 + 0.2 == 0.3));

        BigDecimal sum = new BigDecimal("0.10").add(new BigDecimal("0.20"));
        System.out.println("  BigDecimal: 0.10 + 0.20 = " + sum + "  equals 0.30 ? " + sum.equals(new BigDecimal("0.30")));

        long paise = 12_550;   // Rs 125.50 stored as a whole number of the smallest unit
        System.out.println("  Smallest unit: " + paise + " paise = Rs " + paise / 100 + "." + String.format("%02d", paise % 100));

        double total = 0;
        for (int i = 0; i < 10; i++) total += 0.10;
        System.out.println("  Adding 0.10 ten times as double: " + total + " (not 1.0)");

        System.out.println("  Why not a plain int of rupees? Rs 5 per day works, but real prices have paise,");
        System.out.println("  tax and discounts. Int rupees can't store Rs 0.50. So either:");
        System.out.println("    (a) store the smallest unit (paise) in a long, or");
        System.out.println("    (b) use BigDecimal with an explicit scale and RoundingMode.");
        System.out.println("  Never use double/float for money. Never compare BigDecimals with equals if scale may differ");
        System.out.println("  (use compareTo). Also store the currency, not just the number.");
    }
}