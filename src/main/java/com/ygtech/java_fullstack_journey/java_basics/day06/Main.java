package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Scanner in = new Scanner(System.in);   // don't close a Scanner on System.in

    public static void main(String[] args) {
        Library lib = SeedData.create();
        LocalDate today = LocalDate.now();
        boolean running = true;

        while (running) {
            System.out.println("\n=== Library (today: " + today + ") ===");
            System.out.println("1) List books   2) Search   3) Borrow   4) Return");
            System.out.println("5) Member loans 6) Overdue  7) Stats    8) Advance days   0) Exit");
            System.out.print("> ");
            String choice = in.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> listBooks(lib);
                    case "2" -> showBooks(lib.search(ask("Search text")));
                    case "3" -> {
                        Loan loan = lib.borrow(ask("Book id"), askInt("Member id"), today);
                        System.out.println("Borrowed. Due on " + loan.dueOn());
                    }
                    case "4" -> {
                        int fine = lib.returnBook(ask("Book id"), today);
                        System.out.println(fine == 0 ? "Returned, no fine" : "Returned. Fine: Rs " + fine);
                    }
                    case "5" -> showMemberLoans(lib, askInt("Member id"), today);
                    case "6" -> showOverdue(lib, today);
                    case "7" -> showStats(lib);
                    case "8" -> today = today.plusDays(askInt("Days to advance"));
                    case "0" -> running = false;
                    default -> System.out.println("Unknown option");
                }
            } catch (LibraryException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number");
            }
        }
        System.out.println("Goodbye");
    }

    private static String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.nextLine().trim();
    }

    private static int askInt(String prompt) {
        return Integer.parseInt(ask(prompt));
    }

    private static void listBooks(Library lib) {
        for (Book b : lib.allBooks()) {
            String status = lib.loanFor(b.isbn())
                    .map(l -> "on loan until " + l.dueOn())
                    .orElse("available");
            System.out.printf("%-5s %-18s %-18s %-12s %s%n",
                    b.isbn(), b.title(), b.author(), b.genre(), status);
        }
    }

    private static void showBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No matches");
            return;
        }
        books.forEach(b -> System.out.println(b.isbn() + "  " + b.title() + " by " + b.author()));
    }

    private static void showMemberLoans(Library lib, int memberId, LocalDate today) {
        Member m = lib.getMember(memberId);
        List<Loan> loans = lib.loansOf(memberId);
        System.out.println(m.name() + " has " + loans.size() + " book(s):");
        for (Loan l : loans) {
            System.out.println("  " + lib.getBook(l.isbn()).title() + ", due " + l.dueOn()
                    + (l.isOverdue(today) ? "  ** OVERDUE **" : ""));
        }
    }

    private static void showOverdue(Library lib, LocalDate today) {
        List<Loan> overdue = lib.overdueLoans(today);
        if (overdue.isEmpty()) {
            System.out.println("Nothing is overdue");
            return;
        }
        for (Loan l : overdue) {
            System.out.println(lib.getBook(l.isbn()).title() + " (member " + l.memberId() + "): "
                    + l.daysOverdue(today) + " day(s) late, fine so far Rs "
                    + l.daysOverdue(today) * Library.FINE_PER_DAY);
        }
    }

    private static void showStats(Library lib) {
        System.out.println("Books per genre: " + lib.countByGenre());
        System.out.println("Fines collected: Rs " + lib.totalFinesCollected());
        System.out.println("Most borrowed:");
        for (Map.Entry<String, Long> e : lib.mostBorrowed(3)) {
            System.out.println("  " + lib.getBook(e.getKey()).title() + " x" + e.getValue());
        }
    }
}