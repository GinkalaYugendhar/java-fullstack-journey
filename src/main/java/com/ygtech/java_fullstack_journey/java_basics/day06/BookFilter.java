package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.util.function.Predicate;

/**
 * Extension: composable search. Each optional filter becomes a Predicate<Book>,
 * and they are combined with and(). A null / false argument means "don't filter on this".
 */
public final class BookFilter {
    private BookFilter() {}

    public static Predicate<Book> of(LibraryPlus lib, String genre, Integer fromYear, Integer toYear,
                                     boolean onlyAvailable) {
        Predicate<Book> p = b -> true;   // start with "accept everything"

        if (genre != null) {
            p = p.and(b -> b.genre().equalsIgnoreCase(genre));
        }
        if (fromYear != null) {
            p = p.and(b -> b.year() >= fromYear);
        }
        if (toYear != null) {
            p = p.and(b -> b.year() <= toYear);
        }
        if (onlyAvailable) {
            p = p.and(b -> lib.loanFor(b.isbn()).isEmpty());
        }
        return p;
    }
}