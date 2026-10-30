package com.ygtech.java_fullstack_journey.java_basics.day02;

import java.util.Map;
import java.util.TreeMap;

public class WordFrequency {
    public static void main(String[] args) {
        String text = "to be or not to be that is the question to be";
        Map<String, Integer> freq = new TreeMap<>();   // TreeMap: sorted by key
        for (String word : text.split(" ")) {
            freq.merge(word, 1, Integer::sum);          // add 1, or sum with existing count
        }
        System.out.println(freq);
    }
}