package com.ygtech.java_fullstack_journey.java_basics.day02;

import java.util.List;

public class GenericsDemo {

    // A generic class: T is a placeholder for any type
    static class Box<T> {
        private final T value;
        Box(T value) { this.value = value; }
        T get() { return value; }
    }

    // A generic method with a bound: T must be comparable to itself
    static <T extends Comparable<T>> T maxOf(List<T> items) {
        T best = items.get(0);
        for (T item : items) {
            if (item.compareTo(best) > 0) {
                best = item;
            }
        }
        return best;
    }

    public static void main(String[] args) {
        Box<String> language = new Box<>("Java");
        String s = language.get();                 // no cast needed
        System.out.println(s);

        System.out.println(maxOf(List.of(3, 9, 4)));            // 9
        System.out.println(maxOf(List.of("pear", "apple")));    // pear
    }
}