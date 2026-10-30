package com.ygtech.java_fullstack_journey.java_basics.day02;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // new ArrayList<>(...) because List.of is immutable and we want to remove later
        List<Student> students = new ArrayList<>(List.of(
                new Student(1, "Asha", "Hyderabad", 82.5),
                new Student(2, "Ravi", "Chennai", 74.0),
                new Student(3, "Meera", "Hyderabad", 91.0),
                new Student(4, "Kiran", "Pune", 67.5),
                new Student(5, "Sana", "Chennai", 88.0),
                new Student(6, "Arjun", "Hyderabad", 59.0)
        ));

        // 1. LIST: sort by marks, highest first
        students.sort(Comparator.comparingDouble(Student::marks).reversed());
        System.out.println("Top 3:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  " + students.get(i));
        }

        // 2. SET: unique cities, keeping first-seen order
        Set<String> cities = new LinkedHashSet<>();
        for (Student s : students) {
            cities.add(s.city());
        }
        System.out.println("Cities: " + cities);

        // 3. MAP: group students by city and compute each average
        Map<String, List<Student>> byCity = new HashMap<>();
        for (Student s : students) {
            byCity.computeIfAbsent(s.city(), k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<String, List<Student>> entry : byCity.entrySet()) {
            double total = 0;
            for (Student s : entry.getValue()) {
                total += s.marks();
            }
            System.out.printf("%s: %d students, average %.1f%n",
                    entry.getKey(), entry.getValue().size(), total / entry.getValue().size());
        }

        // 4. ITERATOR: remove safely while looping
        Iterator<Student> it = students.iterator();
        while (it.hasNext()) {
            if (it.next().marks() < 60) {
                it.remove();
            }
        }
        System.out.println("After removing marks below 60: " + students.size() + " students");

        // 5. QUEUE: process in order
        Queue<Student> queue = new ArrayDeque<>(students);
        while (!queue.isEmpty()) {
            Student next = queue.poll();
            System.out.println("Calling for interview: " + next.name());
        }
    }
}