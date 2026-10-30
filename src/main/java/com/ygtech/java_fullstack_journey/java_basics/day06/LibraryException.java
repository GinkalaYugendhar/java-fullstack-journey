package com.ygtech.java_fullstack_journey.java_basics.day06;

public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }

    public static class Duplicate extends LibraryException {
        public Duplicate(String message) { super(message); }
    }

    public static class BookNotFound extends LibraryException {
        public BookNotFound(String isbn) { super("No such book: " + isbn); }
    }

    public static class MemberNotFound extends LibraryException {
        public MemberNotFound(int id) { super("No such member: " + id); }
    }

    public static class BookUnavailable extends LibraryException {
        public BookUnavailable(String title) { super("Already borrowed: " + title); }
    }

    public static class LoanLimitReached extends LibraryException {
        public LoanLimitReached(String name, int max) { super(name + " already has " + max + " books"); }
    }

    public static class NotOnLoan extends LibraryException {
        public NotOnLoan(String isbn) { super("Book is not on loan: " + isbn); }
    }
}