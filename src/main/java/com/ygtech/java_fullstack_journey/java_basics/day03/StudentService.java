package com.ygtech.java_fullstack_journey.java_basics.day03;

import java.util.*;

public class StudentService {
    private final Map<Integer, Student> store = new HashMap<>();

    public StudentService(List<Student> students) {
        for (Student s : students) {
            store.put(s.id(), s);
        }
    }

    public Optional<Student> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    public Student getById(int id) {
        return findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    public static double parseMarks(String raw) throws InvalidMarksException {
        try {
            double marks = Double.parseDouble(raw.trim());
            if (marks < 0 || marks > 100) {
                throw new InvalidMarksException("Marks out of range: " + marks);
            }
            return marks;
        } catch (NumberFormatException e) {
            throw new InvalidMarksException("Not a number: " + raw, e);   // keep the cause
        }
    }
}