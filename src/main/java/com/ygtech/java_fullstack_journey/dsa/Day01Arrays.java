package com.ygtech.java_fullstack_journey.dsa;

public class Day01Arrays {
    public static void main(String[] args) {

        // Problem 1: Find the second-largest distinct element
        int[] numbers = {10, 5, 20, 8, 20, 15};

        int result = secondLargest(numbers);

        System.out.println("Second largest element: " + result);


        // Problem 2: Move all zeroes to the end
        int[] arr = {0, 1, 0, 3, 12};

        System.out.println("Before moving zeroes: "
                + java.util.Arrays.toString(arr));

        moveZeroes(arr);

        System.out.println("After moving zeroes: "
                + java.util.Arrays.toString(arr));
    }

    static int secondLargest(int[] a) {
        int first = Integer.MIN_VALUE;
        int second = Integer.MIN_VALUE;
        for (int x : a) {
            if (x > first) {
                second = first;
                first = x;
            } else if (x > second && x != first) {
                second = x;
            }
        }
        return second;   // stays MIN_VALUE if no second distinct value exists
    }

    static void moveZeroes(int[] a) {
        int write = 0;                        // next position for a non-zero
        for (int read = 0; read < a.length; read++) {
            if (a[read] != 0) {
                int tmp = a[write];
                a[write] = a[read];
                a[read] = tmp;
                write++;
            }
        }
    }
}
