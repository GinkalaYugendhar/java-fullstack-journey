package com.ygtech.java_fullstack_journey.java_basics.day03;

import java.util.List;
import java.util.Optional;

public class ExceptionDemo {

    static class Resource implements AutoCloseable {
        private final String name;

        Resource(String name) {
            this.name = name;
            System.out.println("open " + name);
        }

        void use() { System.out.println("using " + name); }

        @Override
        public void close() { System.out.println("close " + name); }
    }

    static int finallyDemo() {
        try {
            return 1;
        } finally {
            System.out.println("finally runs before the method returns");
        }
    }

    public static void main(String[] args) {
        StudentService service = new StudentService(List.of(
                new Student(1, "Asha", "Hyderabad", 82.5),
                new Student(2, "Ravi", "Chennai", 74.0)
        ));

        // Optional
        Optional<Student> maybe = service.findById(99);
        System.out.println(maybe.map(Student::name).orElse("nobody"));

        // Unchecked custom exception
        try {
            service.getById(99);
        } catch (ResourceNotFoundException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // Checked custom exception, with a preserved cause
        for (String raw : List.of("85.5", "abc", "150")) {
            try {
                System.out.println("Parsed: " + StudentService.parseMarks(raw));
            } catch (InvalidMarksException e) {
                String cause = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none";
                System.out.println("Invalid: " + e.getMessage() + " (cause: " + cause + ")");
            }
        }

        // try-with-resources
        try (Resource a = new Resource("A"); Resource b = new Resource("B")) {
            a.use();
            b.use();
            throw new IllegalStateException("boom");
        } catch (IllegalStateException e) {
            System.out.println("Caught " + e.getMessage());
        } finally {
            System.out.println("finally block");
        }

        System.out.println(finallyDemo());
    }
}