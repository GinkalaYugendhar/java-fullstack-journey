# Day 3 Notes: Lambdas, Streams, Optional and Exceptions

## Lambdas and functional interfaces

A **lambda** is a short, anonymous function that can be passed around like a value. Syntax: `(parameters) -> expression`, or `(parameters) -> { statements; return value; }`.

A lambda needs a **functional interface** as its target type: an interface with exactly **one abstract method**. The lambda supplies the body for that method. `@FunctionalInterface` is a safety net, because the compiler rejects the interface if a second abstract method is added.

| Interface | Method | Meaning | Example |
|-----------|--------|---------|---------|
| `Predicate<T>` | `boolean test(T t)` | A yes/no check | `n -> n > 0` |
| `Function<T,R>` | `R apply(T t)` | Transform T into R | `s -> s.length()` |
| `Consumer<T>` | `void accept(T t)` | Do something with T | `s -> System.out.println(s)` |
| `Supplier<T>` | `T get()` | Produce a T from nothing | `() -> new ArrayList<>()` |
| `BiFunction<T,U,R>` | `R apply(T t, U u)` | Two inputs, one output | `(a, b) -> a + b` |

Also useful: `UnaryOperator<T>` (T in, T out) and `BinaryOperator<T>` (two T in, T out).

**Combining behaviour:**
- `isEven.and(isPositive)` combines two predicates.
- `doubleIt.andThen(plusOne).apply(5)` is 11 (double first, then add 1).
- `doubleIt.compose(plusOne).apply(5)` is 12 (add 1 first, then double).

### Method references

Use `::` when a lambda only calls one existing method.

| Kind | Example | Equivalent lambda |
|------|---------|-------------------|
| Static method | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Method on a specific object | `System.out::println` | `x -> System.out.println(x)` |
| Method on any object of a type | `String::toUpperCase` | `s -> s.toUpperCase()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

### The "effectively final" rule

A lambda can read local variables from the surrounding method only if they never change after assignment. `list.forEach(x -> count++)` is a compile error. The restriction exists because lambdas can run later or on other threads. For counting, use `count()`, `reduce`, or `AtomicInteger`.

## Streams

A stream is a **pipeline** that processes a sequence of elements. It is not a data structure and it never modifies the source collection.

```
source  →  intermediate operations (0 or more)  →  terminal operation (exactly 1)
list.stream()   .filter(...) .map(...) .sorted()        .toList()
```

- **Intermediate operations** return another stream and are **lazy**: `filter`, `map`, `flatMap`, `distinct`, `sorted`, `limit`, `skip`, `peek`.
- **Terminal operations** trigger the work and produce a result: `collect`, `toList`, `forEach`, `reduce`, `count`, `min`, `max`, `findFirst`, `anyMatch`, `allMatch`, `noneMatch`.

### Key operations

- `filter(Predicate)` keeps elements that pass the test.
- `map(Function)` transforms each element. The output type can differ.
- `flatMap(Function)` turns each element into a stream and flattens them into one (use it for "list of lists").
- `reduce(identity, BinaryOperator)` combines all elements into one value.
- `mapToInt` / `mapToDouble` switch to primitive streams, which avoid boxing and give `sum()`, `average()`, `max()`. `average()` returns an `OptionalDouble`, so add `.orElse(0)`.

### Collectors

| Collector | Produces |
|-----------|----------|
| `toList()` / `toSet()` | A List or Set (since Java 16 you can also call `stream.toList()`) |
| `toMap(keyFn, valueFn)` | A Map (throws on duplicate keys) |
| `joining(", ")` | One joined String |
| `groupingBy(keyFn)` | `Map<Key, List<Element>>` |
| `groupingBy(keyFn, downstream)` | Grouping plus a summary per group (`counting()`, `averagingDouble(...)`, `mapping(...)`) |
| `partitioningBy(predicate)` | `Map<Boolean, List<Element>>` (true and false groups) |

**`groupingBy` vs `partitioningBy`:** `groupingBy` splits by any key into any number of groups. `partitioningBy` always gives exactly two groups (true and false), even if one is empty.

**`toMap` vs `groupingBy`:** use `toMap` when each key is unique (and add a merge function as the third argument if duplicates are possible). Use `groupingBy` when many elements share a key.

### Laziness: element by element

Each element travels through the **whole pipeline one at a time**, rather than each stage processing everything first. For `["apple","kiwi","banana","fig"]` with a filter then a map, the print order is: `filter apple, map apple, filter kiwi, map kiwi, filter banana, map banana, filter fig`. With no terminal operation, nothing runs at all.

### Rules and traps

- A stream can be consumed **once**. Reusing it throws `IllegalStateException: stream has already been operated upon or closed`.
- Don't modify the source while streaming over it.
- Keep lambdas free of side effects. Use `collect`, not adding to an outside list inside `map`.
- Avoid `parallelStream()` for now.
- A simple loop is sometimes clearer. Use streams for filter, transform and group pipelines.

## Optional

`Optional<T>` is a box that either holds a value or is empty. It makes "this might not exist" explicit in the method signature.

```java
Optional.of("Asha");            // value must not be null
Optional.ofNullable(null);      // null becomes empty
Optional.empty();
```

| Do this | Example |
|---------|---------|
| Default value | `opt.orElse("unknown")` |
| Default computed lazily | `opt.orElseGet(() -> expensiveDefault())` |
| Throw if missing | `opt.orElseThrow(() -> new ResourceNotFoundException("..."))` |
| Transform if present | `opt.map(String::toUpperCase)` |
| Act if present | `opt.ifPresent(System.out::println)` |

**Don'ts:**
1. Don't call `opt.get()` without checking. It throws `NoSuchElementException` when empty.
2. Don't use `Optional` as a field or method parameter. It is meant for **return types**.
3. Don't write `if (opt.isPresent()) { opt.get() ... }`. Use `map`, `ifPresent` or `orElse`.

In Spring Data you will write `userRepository.findById(id).orElseThrow(...)` constantly.

## Exceptions

```
Throwable
├── Error                      (JVM problems like OutOfMemoryError: don't catch)
└── Exception
    ├── checked exceptions     (IOException, SQLException: must be handled or declared)
    └── RuntimeException       (unchecked: NullPointerException, IllegalArgumentException, ...)
```

- **Checked:** the compiler forces you to catch or declare (`throws`). For recoverable problems the caller must consider.
- **Unchecked** (`RuntimeException`): no forced handling. For programming errors and most business errors.
- **Why Spring prefers unchecked:** it keeps method signatures clean, and a single `@ControllerAdvice` can translate exceptions into HTTP responses (Week 3).

### Tools

- `throw` actually throws an exception object. `throws` in a signature declares that a method may throw one.
- **Multi-catch:** `catch (A | B e)`. You can't multi-catch a parent and child together (`NumberFormatException` is a subclass of `IllegalArgumentException`).
- **Catch order:** specific exceptions first, broader ones last.
- **`finally`** runs whether or not an exception happened, and runs before a `return` completes.
- **try-with-resources** closes anything that implements `AutoCloseable`, in **reverse order**, even if an exception happens, and **before** the `catch` block runs.

### Best practices

1. Never swallow exceptions with an empty `catch`. At minimum, log them.
2. Catch the most specific exception you can handle. Don't catch `Throwable`.
3. Preserve the cause: `new MyException("message", e)`.
4. Don't use exceptions for normal control flow.
5. Name custom exceptions by what went wrong (`ResourceNotFoundException`) and add a helpful message.
6. Never `return` from or throw out of `finally`, because it can hide the real exception.

## What I built

In `java-basics/day03/`:

| File | What it shows |
|------|---------------|
| `FunctionalDemo.java` | The five functional interfaces, `and`, `andThen`, `compose`, a custom `Validator<T>` |
| `Student.java` | The record from Day 2 |
| `StreamExercises.java` | 10 stream exercises plus `flatMap`/`reduce` and `IntStream` bonuses |
| `ResourceNotFoundException.java` | Custom **unchecked** exception |
| `InvalidMarksException.java` | Custom **checked** exception with a cause |
| `StudentService.java` | `Optional` return and `orElseThrow` |
| `ExceptionDemo.java` | Optional, custom exceptions, try-with-resources, `finally` |

Expected stream output highlights: toppers `[ASHA, MEERA, SANA]`, average `77.0`, highest scorer Meera, top 3 `[Meera, Sana, Asha]`, bonus A sum `21`, bonus B `55`. Map entry order may vary because they are `HashMap`s.

Expected exception demo order (and why):
1. `open A`, `open B`, `using A`, `using B`
2. `close B`, `close A` (reverse order, and before the catch runs)
3. `Caught boom`, then `finally block`
4. `finally runs before the method returns`, then `1`

## Things I broke on purpose

- `count++` inside a `forEach` lambda: compile error (not effectively final).
- A second abstract method in `Validator`: `@FunctionalInterface` fails to compile.
- Reusing a stream variable: `IllegalStateException`.
- Duplicate id in exercise 9: `toMap` throws, and a merge function fixes it.
- No terminal operation: nothing runs.

## DSA

**String facts for interviews**
- Strings are immutable. Build in loops with `StringBuilder`, not `+`.
- Compare with `.equals()`, never `==` (`==` compares references).
- `c - 'a'` gives 0 to 25 for lowercase letters.

**Valid Anagram (LeetCode 242)**
- Brute force: sort both strings and compare, O(n log n).
- Better: count letters in a `int[26]`, +1 for `s` and -1 for `t`. All counts must end at 0.
- Complexity: O(n) time, O(1) space (the array is a fixed 26 elements).
- Follow-up: for Unicode input, replace the array with a `HashMap<Character, Integer>`.

**Valid Palindrome (LeetCode 125)**
- Two pointers from both ends, skipping non-alphanumeric characters, comparing lowercase.
- Complexity: O(n) time, O(1) space.
- Tests: `"A man, a plan, a canal: Panama"` is true, `"race a car"` is false, `" "` is true, `"0P"` is false.

## Self-check answers

1. **What makes an interface functional?** It has exactly one abstract method.
2. **`map` vs `flatMap`?** `map` transforms each element into one element. `flatMap` turns each element into a stream and flattens the results into one stream.
3. **Why does nothing happen without a terminal operation?** Intermediate operations are lazy. Only a terminal operation triggers the pipeline.
4. **`toMap` vs `groupingBy`?** `toMap` for unique keys (one value per key). `groupingBy` when many elements share a key.
5. **Why not call `Optional.get()` blindly?** It throws `NoSuchElementException` when empty, which defeats the purpose of `Optional`.
6. **Order of try-with-resources closing?** Reverse order of opening, and before the `catch` block runs.
7. **Why is the anagram solution O(1) space?** The count array is always 26 elements, no matter how long the input is.

## One mistake I made today

> TODO: replace this with a real mistake from today (e.g. reused a stream, forgot a terminal operation, called `get()` on an empty `Optional`, or caught a parent exception before a child).