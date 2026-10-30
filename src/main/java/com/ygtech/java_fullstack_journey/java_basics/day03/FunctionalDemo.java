package com.ygtech.java_fullstack_journey.java_basics.day03;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public class FunctionalDemo {

    // Your own functional interface
    @FunctionalInterface
    interface Validator<T> {
        boolean validate(T value);
    }

    public static void main(String[] args) {
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isPositive = n -> n > 0;
        Function<String, Integer> length = String::length;
        Consumer<String> printer = System.out::println;
        Supplier<List<String>> listMaker = ArrayList::new;
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        UnaryOperator<String> shout = s -> s.toUpperCase() + "!";

        System.out.println(isEven.test(4));                       // true
        System.out.println(length.apply("lambda"));               // 6
        printer.accept("hello");                                  // hello
        System.out.println(listMaker.get().size());               // 0
        System.out.println(add.apply(3, 4));                      // 7
        System.out.println(shout.apply("java"));                  // JAVA!

        // Combining behaviour
        System.out.println(isEven.and(isPositive).test(-4));      // false
        Function<Integer, Integer> doubleIt = x -> x * 2;
        Function<Integer, Integer> plusOne = x -> x + 1;
        System.out.println(doubleIt.andThen(plusOne).apply(5));   // 11 (double, then +1)
        System.out.println(doubleIt.compose(plusOne).apply(5));   // 12 (+1, then double)

        // Using your own interface
        Validator<String> notBlank = s -> !s.isBlank();
        System.out.println(notBlank.validate("  "));              // false
    }
}