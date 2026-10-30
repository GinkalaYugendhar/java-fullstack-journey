package com.ygtech.java_fullstack_journey.java_basics.day03;

import java.util.*;
import static java.util.stream.Collectors.*;

public class StreamExercises {
    public static void main(String[] args) {
        List<Student> students = List.of(
                new Student(1, "Asha", "Hyderabad", 82.5),
                new Student(2, "Ravi", "Chennai", 74.0),
                new Student(3, "Meera", "Hyderabad", 91.0),
                new Student(4, "Kiran", "Pune", 67.5),
                new Student(5, "Sana", "Chennai", 88.0),
                new Student(6, "Arjun", "Hyderabad", 59.0)
        );

        // 1. Names above 80, uppercase, sorted
        List<String> toppers = students.stream()
                .filter(s -> s.marks() > 80)
                .map(s -> s.name().toUpperCase())
                .sorted()
                .toList();
        System.out.println("1. " + toppers);

        // 2. Average marks
        double avg = students.stream().mapToDouble(Student::marks).average().orElse(0);
        System.out.println("2. " + avg);

        // 3. Highest scorer (max returns an Optional)
        students.stream()
                .max(Comparator.comparingDouble(Student::marks))
                .ifPresent(s -> System.out.println("3. " + s.name()));

        // 4. Names grouped by city
        Map<String, List<String>> namesByCity = students.stream()
                .collect(groupingBy(Student::city, mapping(Student::name, toList())));
        System.out.println("4. " + namesByCity);

        // 5. Count per city
        Map<String, Long> countByCity = students.stream()
                .collect(groupingBy(Student::city, counting()));
        System.out.println("5. " + countByCity);

        // 6. Average marks per city
        Map<String, Double> avgByCity = students.stream()
                .collect(groupingBy(Student::city, averagingDouble(Student::marks)));
        System.out.println("6. " + avgByCity);

        // 7. Pass (60 or more) and fail
        Map<Boolean, List<String>> passFail = students.stream()
                .collect(partitioningBy(s -> s.marks() >= 60, mapping(Student::name, toList())));
        System.out.println("7. " + passFail);

        // 8. All names joined into one string
        String joined = students.stream().map(Student::name).collect(joining(", "));
        System.out.println("8. " + joined);

        // 9. Map of id to name
        Map<Integer, String> idToName = students.stream()
                .collect(toMap(Student::id, Student::name));
        System.out.println("9. " + idToName);

        // 10. Top 3 by marks
        List<String> top3 = students.stream()
                .sorted(Comparator.comparingDouble(Student::marks).reversed())
                .limit(3)
                .map(Student::name)
                .toList();
        System.out.println("10. " + top3);

        // Bonus A: flatMap and reduce
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3), List.of(4, 5, 6));
        List<Integer> flat = nested.stream().flatMap(List::stream).toList();
        int sum = flat.stream().reduce(0, Integer::sum);
        System.out.println("A. " + flat + " sum=" + sum);

        // Bonus B: primitive stream
        System.out.println("B. " + java.util.stream.IntStream.rangeClosed(1, 5).map(n -> n * n).sum());
    }
}