package com.ygtech.java_fullstack_journey.dsa;

public class Day04Strings {

    // ---------- Problem 1: Longest Common Prefix ----------

    // Vertical scanning: compare column by column. O(S) time
    static String lcpVertical(String[] strs) {
        if (strs == null || strs.length == 0) return "";
        String first = strs[0];
        for (int i = 0; i < first.length(); i++) {
            char c = first.charAt(i);
            for (int j = 1; j < strs.length; j++) {
                if (i == strs[j].length() || strs[j].charAt(i) != c) {
                    return first.substring(0, i);
                }
            }
        }
        return first;
    }

    // Shrink the prefix until every word starts with it. O(S) time, O(1) extra space
    static String longestCommonPrefix(String[] strs) {
        if (strs == null || strs.length == 0) return "";
        String prefix = strs[0];
        for (int i = 1; i < strs.length; i++) {
            while (!strs[i].startsWith(prefix)) {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty()) return "";
            }
        }
        return prefix;
    }

    // ---------- Problem 2: Reverse Words in a String ----------

    // trim + split on whitespace runs + StringBuilder. O(n) time, O(n) space
    static String reverseWords(String s) {
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    // Follow-up: in place on a char[] (reverse everything, then reverse each word back).
    // Assumes words are separated by single spaces, with no leading/trailing spaces.
    static void reverseWordsInPlace(char[] a) {
        reverse(a, 0, a.length - 1);
        int start = 0;
        for (int i = 0; i <= a.length; i++) {
            if (i == a.length || a[i] == ' ') {
                reverse(a, start, i - 1);
                start = i + 1;
            }
        }
    }

    private static void reverse(char[] a, int l, int r) {
        while (l < r) {
            char tmp = a[l];
            a[l++] = a[r];
            a[r--] = tmp;
        }
    }

    // ---------- Tests ----------

    static int failures = 0;

    static void check(String label, String actual, String expected) {
        boolean ok = actual.equals(expected);
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + label + " -> \"" + actual + "\""
                + (ok ? "" : " (expected \"" + expected + "\")"));
    }

    public static void main(String[] args) {
        System.out.println("=== Longest Common Prefix (shrink / vertical) ===");
        Object[][] lcpCases = {
                {new String[]{"flower", "flow", "flight"}, "fl"},
                {new String[]{"dog", "racecar", "car"},    ""},       // no common prefix
                {new String[]{"alone"},                    "alone"},  // single word
                {new String[]{},                           ""},       // empty array
                {new String[]{"abc", "", "abd"},           ""},       // empty string in the array
                {new String[]{"same", "same", "same"},     "same"},   // identical words
                {new String[]{"ab", "abc", "abcd"},        "ab"},     // one word is the prefix
        };
        for (Object[] c : lcpCases) {
            String[] input = (String[]) c[0];
            String expected = (String) c[1];
            String name = java.util.Arrays.toString(input);
            check("[shrink]   " + name, longestCommonPrefix(input), expected);
            check("[vertical] " + name, lcpVertical(input), expected);
        }

        System.out.println("\n=== Reverse Words in a String ===");
        check("\" the sky is blue \"",   reverseWords("  the sky is blue "), "blue is sky the");
        check("\"hello world\"",         reverseWords("hello world"), "world hello");
        check("single word",             reverseWords("hello"), "hello");
        check("only spaces",             reverseWords("   "), "");
        check("multiple inner spaces",   reverseWords("a   good   example"), "example good a");
        check("leading and trailing",    reverseWords("   Alice  Bob   "), "Bob Alice");

        System.out.println("\n=== Reverse Words in place (char[] follow-up) ===");
        char[] chars = "the sky is blue".toCharArray();
        reverseWordsInPlace(chars);
        check("\"the sky is blue\"", new String(chars), "blue is sky the");
        char[] one = "hello".toCharArray();
        reverseWordsInPlace(one);
        check("single word", new String(one), "hello");

        System.out.println("\n" + (failures == 0 ? "All tests passed." : failures + " test(s) failed."));
    }
}