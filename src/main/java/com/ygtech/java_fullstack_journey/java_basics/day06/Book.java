package com.ygtech.java_fullstack_journey.java_basics.day06;

public record Book(String isbn, String title, String author, String genre, int year) {
    public Book {   // compact constructor: validate before fields are assigned
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
    }
}