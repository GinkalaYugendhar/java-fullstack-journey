package com.ygtech.java_fullstack_journey.dsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Day03Strings {

    // ---------- Problem 1: Valid Anagram ----------

    // Brute force: sort both and compare. O(n log n) time
    static boolean isAnagramSort(String s, String t) {
        if (s.length() != t.length()) return false;
        char[] a = s.toCharArray();
        char[] b = t.toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    // Count letters (lowercase a-z only). O(n) time, O(1) space
    static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;
            count[t.charAt(i) - 'a']--;
        }
        for (int c : count) {
            if (c != 0) return false;
        }
        return true;
    }

    // Follow-up: Unicode input. HashMap instead of a fixed 26-element array
    static boolean isAnagramUnicode(String s, String t) {
        if (s.length() != t.length()) return false;
        Map<Character, Integer> count = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            count.merge(s.charAt(i), 1, Integer::sum);
            count.merge(t.charAt(i), -1, Integer::sum);
        }
        for (int c : count.values()) {
            if (c != 0) return false;
        }
        return true;
    }

    // ---------- Problem 2: Valid Palindrome ----------

    // Two pointers, skipping non-alphanumeric characters. O(n) time, O(1) space
    static boolean isPalindrome(String s) {
        int l = 0, r = s.length() - 1;
        while (l < r) {
            while (l < r && !Character.isLetterOrDigit(s.charAt(l))) l++;
            while (l < r && !Character.isLetterOrDigit(s.charAt(r))) r--;
            if (Character.toLowerCase(s.charAt(l)) != Character.toLowerCase(s.charAt(r))) {
                return false;
            }
            l++;
            r--;
        }
        return true;
    }

    // ---------- Tests ----------

    static int failures = 0;

    static void check(String label, boolean actual, boolean expected) {
        boolean ok = actual == expected;
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + label + " -> " + actual
                + (ok ? "" : " (expected " + expected + ")"));
    }

    public static void main(String[] args) {
        System.out.println("=== Valid Anagram (sort / count / unicode) ===");
        String[][] anagramCases = {
                {"anagram", "nagaram"},   // true
                {"rat", "car"},           // false
                {"a", "a"},               // true
                {"ab", "a"},              // false (different lengths)
                {"aab", "abb"},           // false (same length, different counts)
                {"", ""},                 // true (empty strings)
        };
        boolean[] anagramExpected = {true, false, true, false, false, true};

        for (int i = 0; i < anagramCases.length; i++) {
            String s = anagramCases[i][0], t = anagramCases[i][1];
            String name = "\"" + s + "\" vs \"" + t + "\"";
            check("[sort]    " + name, isAnagramSort(s, t), anagramExpected[i]);
            check("[count]   " + name, isAnagram(s, t), anagramExpected[i]);
            check("[unicode] " + name, isAnagramUnicode(s, t), anagramExpected[i]);
        }
        // Unicode-only case: the 26-letter array version would crash here
        check("[unicode] \"héllo\" vs \"lléoh\"", isAnagramUnicode("h\u00e9llo", "ll\u00e9oh"), false);
        check("[unicode] \"héllo\" vs \"olléh\"", isAnagramUnicode("h\u00e9llo", "oll\u00e9h"), true);

        System.out.println("\n=== Valid Palindrome ===");
        check("\"A man, a plan, a canal: Panama\"", isPalindrome("A man, a plan, a canal: Panama"), true);
        check("\"race a car\"",                     isPalindrome("race a car"), false);
        check("\" \" (nothing to compare)",         isPalindrome(" "), true);
        check("\"0P\"",                             isPalindrome("0P"), false);
        check("\"\" (empty)",                       isPalindrome(""), true);
        check("\".,\" (only punctuation)",          isPalindrome(".,"), true);
        check("\"a\" (single char)",                isPalindrome("a"), true);
        check("\"No lemon, no melon\"",             isPalindrome("No lemon, no melon"), true);

        System.out.println("\n" + (failures == 0 ? "All tests passed." : failures + " test(s) failed."));
    }
}