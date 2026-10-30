package com.ygtech.java_fullstack_journey.dsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Day02Arrays {

    // ---------- Problem 1: Two Sum ----------

    // Brute force: O(n^2) time, O(1) space
    static int[] twoSumBrute(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1, -1};
    }

    // HashMap: O(n) time, O(n) space
    static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();   // value -> index
        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];
            if (seen.containsKey(need)) {
                return new int[]{seen.get(need), i};
            }
            seen.put(nums[i], i);
        }
        return new int[]{-1, -1};   // no answer
    }

    // ---------- Problem 2: Best Time to Buy and Sell Stock ----------

    // O(n) time, O(1) space
    static int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int best = 0;
        for (int p : prices) {
            if (p < minPrice) {
                minPrice = p;                         // new cheapest buying day
            } else {
                best = Math.max(best, p - minPrice);  // profit if sold today
            }
        }
        return best;
    }

    // ---------- Tests ----------

    static int failures = 0;

    static void check(String label, int[] actual, int[] expected) {
        boolean ok = Arrays.equals(actual, expected);
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + label
                + " -> " + Arrays.toString(actual)
                + (ok ? "" : " (expected " + Arrays.toString(expected) + ")"));
    }

    static void check(String label, int actual, int expected) {
        boolean ok = actual == expected;
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + label
                + " -> " + actual + (ok ? "" : " (expected " + expected + ")"));
    }

    public static void main(String[] args) {
        System.out.println("=== Two Sum ===");
        for (int pass = 0; pass < 2; pass++) {
            boolean brute = pass == 0;
            String tag = brute ? "[brute] " : "[map]   ";

            check(tag + "example",    brute ? twoSumBrute(new int[]{2, 7, 11, 15}, 9) : twoSum(new int[]{2, 7, 11, 15}, 9), new int[]{0, 1});
            check(tag + "duplicates", brute ? twoSumBrute(new int[]{3, 3}, 6)         : twoSum(new int[]{3, 3}, 6),         new int[]{0, 1});
            check(tag + "negatives",  brute ? twoSumBrute(new int[]{-3, 4, 3, 90}, 0) : twoSum(new int[]{-3, 4, 3, 90}, 0), new int[]{0, 2});
            check(tag + "no pair",    brute ? twoSumBrute(new int[]{1, 2, 3}, 100)    : twoSum(new int[]{1, 2, 3}, 100),    new int[]{-1, -1});
            check(tag + "target mid", brute ? twoSumBrute(new int[]{3, 2, 4}, 6)      : twoSum(new int[]{3, 2, 4}, 6),      new int[]{1, 2});
        }

        System.out.println("\n=== Best Time to Buy and Sell Stock ===");
        check("example",             maxProfit(new int[]{7, 1, 5, 3, 6, 4}), 5);
        check("strictly decreasing", maxProfit(new int[]{7, 6, 4, 3, 1}),    0);
        check("single element",      maxProfit(new int[]{5}),                0);
        check("empty array",         maxProfit(new int[]{}),                 0);
        check("strictly increasing", maxProfit(new int[]{1, 2, 3, 4, 5}),    4);
        check("all same",            maxProfit(new int[]{4, 4, 4}),          0);

        System.out.println("\n" + (failures == 0 ? "All tests passed." : failures + " test(s) failed."));
    }
}