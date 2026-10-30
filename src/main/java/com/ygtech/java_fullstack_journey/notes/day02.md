# Day 2 Notes: Collections, Generics and Iteration

## The four collection families

| Interface | Meaning | Duplicates? | Ordered? |
|-----------|---------|-------------|----------|
| `List` | Sequence accessed by index | Yes | Yes (insertion order) |
| `Set` | Unique elements | No | Depends on implementation |
| `Map` | Key to value pairs (part of the framework, but not a `Collection`) | Keys unique | Depends on implementation |
| `Queue` / `Deque` | Elements waiting to be processed | Yes | Yes (by queue rules) |

**Program to the interface:** `List<String> names = new ArrayList<>();`. This is polymorphism from Day 1, and the implementation can be swapped by changing one word.

## ArrayList vs LinkedList

- **ArrayList** is a resizable array. When full, it allocates a bigger array (about 1.5x) and copies the elements over.
    - `get(i)`: O(1), a direct array lookup
    - `add(x)` at the end: O(1) amortized
    - Insert or remove in the middle: O(n), because elements shift
- **LinkedList** is a chain of nodes with previous and next pointers.
    - `get(i)`: O(n), because it must walk the chain
    - Insert or remove at a node already held: O(1)
- **When to use which:** `ArrayList` almost always. Arrays are cache-friendly, so it usually beats `LinkedList` even where theory says otherwise.

## How HashMap works inside

1. It holds an array of buckets (16 by default).
2. For a key, it computes `hashCode()`, mixes the bits, and picks a bucket index.
3. If two keys land in the same bucket (a **collision**), they are stored together as a linked list. From Java 8, a long bucket (8+ entries, in a large enough table) converts to a balanced tree.
4. When the map is 75% full (the **load factor**), it doubles the array and redistributes the entries (**resizing**).
5. To find a key, it uses `hashCode()` to find the bucket, then `equals()` to find the exact key inside it.

Keys must have consistent `equals()` and `hashCode()`. `String` and `Integer` already do. (Covered fully on Day 8.)

## Set and Map implementations

| Class | Order | add/contains speed | Use when |
|-------|-------|--------------------|----------|
| `HashSet` / `HashMap` | None guaranteed | O(1) average | Only uniqueness or lookup is needed |
| `LinkedHashSet` / `LinkedHashMap` | Insertion order | O(1) average | Uniqueness plus keeping order |
| `TreeSet` / `TreeMap` | Sorted | O(log n) | Sorted unique values or sorted keys |

A `HashSet` is secretly a `HashMap` whose keys are the elements.

## Queue and Deque

- **Queue:** FIFO. `offer()` adds, `poll()` removes from the head.
- **Deque:** add and remove at both ends. `ArrayDeque` is the best choice as both a queue and a stack (`push`/`pop`). Avoid the old `Stack` class.
- **PriorityQueue:** a min-heap, so `poll()` returns the smallest element first. Iterating it does **not** give sorted order.

## Which one do I pick?

| I need to... | Use |
|--------------|-----|
| Keep items in order and access by index | `ArrayList` |
| Remove duplicates | `HashSet` (or `LinkedHashSet` to keep order) |
| Look up a value by key | `HashMap` |
| Keep keys sorted | `TreeMap` / `TreeSet` |
| Process first-come, first-served | `ArrayDeque` as a Queue |
| Always get the smallest or largest next | `PriorityQueue` |
| Use as a stack (undo, brackets) | `ArrayDeque` with `push`/`pop` |

## Two traps

- `List.of(...)` is **immutable**. `add()` throws `UnsupportedOperationException`. For a modifiable copy: `new ArrayList<>(List.of(...))`.
- Collections hold **objects**, not primitives. `List<int>` won't compile, so use `List<Integer>`. Autoboxing converts automatically. Compare wrapper objects with `.equals()`, not `==`.

## Generics

Generics let a class or method work with any type while staying type-safe.

- **Without generics** (raw type): `String s = (String) raw.get(1);` compiles and then crashes with `ClassCastException` at runtime.
- **With generics:** `names.add(42);` is a compile error, so the compiler catches the mistake early.
- `<>` (the diamond operator) lets Java infer the type on the right-hand side.
- **Bounds:** `<T extends Comparable<T>>` tells the compiler that `T` has `compareTo`. `maxOf` needs it because `compareTo` exists only on `Comparable` types.
- `Box<int>` fails because generics work with objects only. Use `Box<Integer>`.
- `maxOf` on a list of `Object` fails because `Object` is not `Comparable<Object>`.

## Iteration and safe removal

Removing from a list inside a for-each loop can throw `ConcurrentModificationException`, because the list detects it was changed mid-iteration. Sometimes it silently skips elements instead, which is worse, so never rely on it.

Two safe options:

```java
Iterator<Student> it = students.iterator();
while (it.hasNext()) {
    if (it.next().marks() < 60) it.remove();
}

students.removeIf(s -> s.marks() < 60);
```

## What I built

In `java-basics/day02/`:

| File | What it shows |
|------|---------------|
| `GenericsDemo.java` | Generic class `Box<T>` and bounded generic method `maxOf` |
| `Student.java` | A `record` (immutable data class with auto-generated constructor, accessors like `name()`, `equals`, `hashCode`, `toString`) |
| `Main.java` | List sorting, `LinkedHashSet`, `HashMap` grouping, `Iterator.remove()`, `ArrayDeque` as a queue |
| `WordFrequency.java` | `TreeMap` with `merge(word, 1, Integer::sum)` |

Useful methods learned:

- `computeIfAbsent(key, k -> new ArrayList<>())` replaces the get, check null, put pattern
- `Comparator.comparingDouble(Student::marks).reversed()` builds a sorting rule, where `Student::marks` is a method reference
- `merge(key, 1, Integer::sum)` adds 1 or sums with the existing count

Expected output notes:

- Top 3: Meera (91.0), Sana (88.0), Asha (82.5)
- Averages: Hyderabad 77.5, Chennai 81.0, Pune 67.5 (the order can vary with `HashMap`)
- After removing marks below 60: 5 students
- Word frequency: `{be=3, is=1, not=1, or=1, question=1, that=1, the=1, to=3}`

## DSA

**Two Sum (LeetCode 1)**
- Brute force: two nested loops, O(n²) time.
- Better: store `value -> index` in a `HashMap`. For each number, look up `target - number`, which takes O(1).
- Complexity: O(n) time, O(n) space. This is the "trade space for time" pattern.
- Edge cases: duplicates (`[3, 3]`, target 6), negatives, no valid pair.

**Best Time to Buy and Sell Stock (LeetCode 121)**
- Scan once, tracking the cheapest price so far, and at each day check the profit if sold today.
- Complexity: O(n) time, O(1) space.
- Edge cases: strictly decreasing prices (answer 0), a single element.

## Self-check answers

1. **Why declare as `List` instead of `ArrayList`?** To program to the interface, so the implementation can change without touching the rest of the code.
2. **Why is `get(i)` O(1) on `ArrayList` but O(n) on `LinkedList`?** An array can jump straight to an index, while a linked list must walk node by node.
3. **What happens when two keys collide in a `HashMap`?** They share a bucket (as a linked list, or a tree if the bucket gets long), and `equals()` picks the exact key.
4. **Why can't you write `List<int>`?** Generics work with objects, so use the wrapper type `Integer`.
5. **Why does removing in a for-each throw, and what are two safe alternatives?** The list detects modification during iteration. Use `Iterator.remove()` or `removeIf`.
6. **Why does Two Sum with a `HashMap` beat brute force?** Each lookup is O(1), so the whole thing is O(n) instead of O(n²).

## One mistake I made today

> TODO: replace this with a real mistake from today (e.g. forgot `new ArrayList<>(List.of(...))` and got `UnsupportedOperationException`, compared `Integer` objects with `==`, or removed inside a for-each loop).