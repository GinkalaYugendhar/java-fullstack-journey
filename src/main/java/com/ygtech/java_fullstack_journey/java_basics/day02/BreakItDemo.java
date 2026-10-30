package com.ygtech.java_fullstack_journey.java_basics.day02;

import java.util.*;

public class BreakItDemo {

    static List<Student> freshList() {
        // new ArrayList<>(...) so we can modify it
        return new ArrayList<>(List.of(
                new Student(1, "Asha", "Hyderabad", 82.5),
                new Student(2, "Ravi", "Chennai", 74.0),
                new Student(3, "Meera", "Hyderabad", 91.0),
                new Student(4, "Kiran", "Pune", 67.5),
                new Student(5, "Sana", "Chennai", 88.0),
                new Student(6, "Arjun", "Hyderabad", 59.0)   // the one to remove (last element)
        ));
    }

    public static void main(String[] args) {

        // 1. BREAK IT: remove inside a for-each loop -> ConcurrentModificationException
        System.out.println("--- 1. Removing inside for-each ---");
        List<Student> broken = freshList();
        try {
            for (Student s : broken) {
                if (s.marks() < 60) {
                    broken.remove(s);   // modifying while iterating
                }
            }
            System.out.println("No exception this time (see demo 2 for why that's dangerous)");
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught: " + e);
        }

        // 2. WORSE: sometimes it does NOT throw, it silently skips elements
        System.out.println("\n--- 2. Silent skip (no exception!) ---");
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4));
        for (Integer n : nums) {
            if (n == 2) {
                nums.remove(n);   // removes 2, loop then skips 3 and stops
            }
            System.out.println("visited " + n);
        }
        System.out.println("Result: " + nums + "  (3 was never visited)");

        // 3. FIX A: Iterator.remove()
        System.out.println("\n--- 3. Fix A: Iterator.remove() ---");
        List<Student> a = freshList();
        Iterator<Student> it = a.iterator();
        while (it.hasNext()) {
            if (it.next().marks() < 60) {
                it.remove();
            }
        }
        System.out.println("Remaining: " + a.size() + " students");

        // 4. FIX B: removeIf (one line, same thing)
        System.out.println("\n--- 4. Fix B: removeIf ---");
        List<Student> b = freshList();
        b.removeIf(s -> s.marks() < 60);
        System.out.println("Remaining: " + b.size() + " students");
    }
}