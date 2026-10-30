package com.ygtech.java_fullstack_journey.java_basics.day06;

public final class SeedData {
    private SeedData() {}

    public static Library create() {
        Library lib = new Library();
        lib.addBook(new Book("B101", "Effective Java",    "Joshua Bloch",      "Programming", 2018));
        lib.addBook(new Book("B102", "Clean Code",        "Robert C. Martin",  "Programming", 2008));
        lib.addBook(new Book("B103", "Design Patterns",   "Erich Gamma",       "Programming", 1994));
        lib.addBook(new Book("B104", "The Alchemist",     "Paulo Coelho",      "Fiction",     1988));
        lib.addBook(new Book("B105", "Brave New World",   "Aldous Huxley",     "Fiction",     1932));
        lib.addBook(new Book("B106", "Sapiens",           "Yuval Noah Harari", "History",     2011));
        lib.addBook(new Book("B107", "The Hobbit",        "J.R.R. Tolkien",    "Fiction",     1937));

        lib.addMember(new Member(1, "Asha"));
        lib.addMember(new Member(2, "Ravi"));
        lib.addMember(new Member(3, "Meera"));
        return lib;
    }
}