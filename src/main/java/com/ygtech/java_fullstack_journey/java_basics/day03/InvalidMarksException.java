package com.ygtech.java_fullstack_journey.java_basics.day03;

public class InvalidMarksException extends Exception {
    public InvalidMarksException(String message) {
        super(message);
    }

    public InvalidMarksException(String message, Throwable cause) {
        super(message, cause);
    }
}