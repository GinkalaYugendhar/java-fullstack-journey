package com.ygtech.java_fullstack_journey.dsa;

import java.util.*;

public class Day06Hashing {

    // ---------- Problem 1: Group Anagrams ----------

    // Idea A: the sorted word is the key. O(n * k log k)
    static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String s : strs) {
            char[] chars = s.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    // Idea B: the 26 letter counts (as a String) are the key. O(n * k). Lowercase a-z only
    static List<List<String>> groupAnagramsByCount(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String s : strs) {
            int[] count = new int[26];
            for (char c : s.toCharArray()) {
                count[c - 'a']++;
            }
            String key = Arrays.toString(count);   // arrays can't be good keys, Strings can
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    // The mistake to see once: int[] as a key uses identity hashing, so nothing ever groups
    static int groupsWithRawArrayKey(String[] strs) {
        Map<int[], List<String>> groups = new HashMap<>();
        for (String s : strs) {
            int[] count = new int[26];
            for (char c : s.toCharArray()) {
                count[c - 'a']++;
            }
            groups.computeIfAbsent(count, k -> new ArrayList<>()).add(s);
        }
        return groups.size();
    }

    // ---------- Problem 2: Longest Consecutive Sequence ----------

    // Sort version, written first: O(n log n)
    static int longestConsecutiveSort(int[] nums) {
        if (nums.length == 0) return 0;
        int[] a = nums.clone();   // don't mutate the caller's array
        Arrays.sort(a);
        int best = 1, cur = 1;
        for (int i = 1; i < a.length; i++) {
            if (a[i] == a[i - 1]) continue;                    // duplicate: skip
            if ((long) a[i] - a[i - 1] == 1) cur++;            // continues the run
            else cur = 1;                                      // run broken
            best = Math.max(best, cur);
        }
        return best;
    }

    // HashSet version: O(n). Only walk a run from its start (n - 1 not in the set)
    static int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int n : nums) {
            set.add(n);
        }

        int best = 0;
        for (int n : set) {
            if (!set.contains(n - 1)) {            // n is the start of a run
                int cur = n;
                int len = 1;
                while (set.contains(cur + 1)) {    // walk the run
                    cur++;
                    len++;
                }
                best = Math.max(best, len);
            }
        }
        return best;
    }

    // ---------- Bonus 1: Contains Duplicate ----------

    static boolean containsDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int n : nums) {
            if (!seen.add(n)) return true;   // add() returns false if it was already there
        }
        return false;
    }

    // ---------- Bonus 2: Subarray Sum Equals K (prefix sums + HashMap) ----------

    static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> prefixCount = new HashMap<>();
        prefixCount.put(0, 1);   // the empty prefix, so subarrays starting at index 0 are counted
        int sum = 0, result = 0;
        for (int n : nums) {
            sum += n;
            result += prefixCount.getOrDefault(sum - k, 0);   // earlier prefixes that leave exactly k
            prefixCount.merge(sum, 1, Integer::sum);
        }
        return result;
    }

    // ---------- Tests ----------

    static int failures = 0;

    static void check(String label, String actual, String expected) {
        boolean ok = actual.equals(expected);
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + label + " -> " + actual
                + (ok ? "" : " (expected " + expected + ")"));
    }

    static void check(String label, int actual, int expected) {
        check(label, String.valueOf(actual), String.valueOf(expected));
    }

    static void check(String label, boolean actual, boolean expected) {
        check(label, String.valueOf(actual), String.valueOf(expected));
    }

    // Group and word order are not guaranteed, so sort everything before comparing
    static String normalize(List<List<String>> groups) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> g : groups) {
            List<String> sorted = new ArrayList<>(g);
            Collections.sort(sorted);
            copy.add(sorted);
        }
        copy.sort(Comparator.comparing(g -> g.get(0)));
        return copy.toString();
    }

    public static void main(String[] args) {
        System.out.println("=== Group Anagrams (sorted key / count key) ===");
        Object[][] anagramCases = {
                {new String[]{"eat", "tea", "tan", "ate", "nat", "bat"}, "[[ate, eat, tea], [bat], [nat, tan]]"},
                {new String[]{""},                "[[]]"},   // one group holding one empty string prints as [[]]
                {new String[]{"a"},               "[[a]]"},
                {new String[]{"a", "b", "c"},     "[[a], [b], [c]]"},
                {new String[]{"abc", "abc"},      "[[abc, abc]]"},
                {new String[]{"ab", "ba", "abc"}, "[[ab, ba], [abc]]"},
        };
        for (Object[] c : anagramCases) {
            String[] input = (String[]) c[0];
            String expected = (String) c[1];
            String name = Arrays.toString(input);
            check("[sorted] " + name, normalize(groupAnagrams(input)), expected);
            check("[count]  " + name, normalize(groupAnagramsByCount(input)), expected);
        }
        String[] six = {"eat", "tea", "tan", "ate", "nat", "bat"};
        check("raw int[] key: number of groups (should be 3, but is one per word)",
                groupsWithRawArrayKey(six), 6);

        System.out.println("\n=== Longest Consecutive Sequence (sort / set) ===");
        Object[][] lcsCases = {
                {new int[]{100, 4, 200, 1, 3, 2},                   4},
                {new int[]{},                                       0},
                {new int[]{5},                                      1},
                {new int[]{0, 0},                                   1},   // duplicates
                {new int[]{-1, 0, 1},                               3},   // negatives
                {new int[]{5, 4, 3, 2, 1},                          5},   // full run, unsorted
                {new int[]{1, 2, 0, 1},                             3},   // duplicate inside a run
                {new int[]{9, 1, 4, 7, 3, -1, 0, 5, 8, -1, 6},      7},   // 3..9
                {new int[]{10, 30, 20},                             1},   // no consecutive pair
        };
        for (Object[] c : lcsCases) {
            int[] input = (int[]) c[0];
            int expected = (int) c[1];
            String name = Arrays.toString(input);
            check("[sort] " + name, longestConsecutiveSort(input), expected);
            check("[set]  " + name, longestConsecutive(input), expected);
        }

        System.out.println("\n=== Bonus: Contains Duplicate ===");
        check("[1, 2, 3, 1]", containsDuplicate(new int[]{1, 2, 3, 1}), true);
        check("[1, 2, 3, 4]", containsDuplicate(new int[]{1, 2, 3, 4}), false);
        check("[]",           containsDuplicate(new int[]{}), false);

        System.out.println("\n=== Bonus: Subarray Sum Equals K ===");
        check("[1, 1, 1], k=2",   subarraySum(new int[]{1, 1, 1}, 2), 2);
        check("[1, 2, 3], k=3",   subarraySum(new int[]{1, 2, 3}, 3), 2);
        check("[1, -1, 0], k=0",  subarraySum(new int[]{1, -1, 0}, 0), 3);
        check("[1], k=0",         subarraySum(new int[]{1}, 0), 0);
        check("[], k=0",          subarraySum(new int[]{}, 0), 0);

        System.out.println("\n" + (failures == 0 ? "All tests passed." : failures + " test(s) failed."));
    }
}