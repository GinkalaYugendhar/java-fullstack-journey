# Day 6 Notes: Console Library Manager and Hashing

## What I built

A complete console application that uses Days 1 to 5 together: OOP, collections, streams, `Optional`, exceptions, records and `java.time`. The goal was to **design with collections** rather than just use them.

### Requirements
- A library has books and members.
- A member can borrow a book for **14 days**, and hold at most **3 books** at a time.
- A book can be on loan to **only one member** at a time.
- Returning late costs a fine of **₹5 per day**.
- Users can search books (title or author), see overdue loans, and see statistics (books per genre, most borrowed, total fines).

### Files in `day06/`

| File | Role |
|------|------|
| `Book.java` | Record with a compact constructor that validates ISBN and title |
| `Member.java` | Record (`id`, `name`) |
| `Loan.java` | Record with `daysOverdue(today)` and `isOverdue(today)` |
| `ReturnedLoan.java` | Record: the loan, return date, and fine |
| `LibraryException.java` | One `RuntimeException` parent with six nested specific types |
| `Library.java` | All business logic (no `System.out`, no `Scanner`) |
| `SeedData.java` | Sample books and members |
| `LibraryDemo.java` | Scripted run with predictable output |
| `Main.java` | Interactive console menu |

## Design decisions (and why)

### 1. Immutable data, mutable state kept separately
`Book` and `Member` are records: facts that don't change. Whether a book is on loan is *state*, so it lives in a separate `Loan` object inside `Library`, not in a mutable `boolean available` on `Book`. This avoids the "two places disagree" bug.

### 2. Pick each collection for what it must do

| Data | Collection | Why |
|------|-----------|-----|
| Books | `LinkedHashMap<String, Book>` | O(1) lookup by ID, and listing keeps insertion order |
| Members | `LinkedHashMap<Integer, Member>` | Same reasoning |
| Active loans | `HashMap<String, Loan>` keyed by book ID | A book can have only one loan, so **the key enforces the rule**. "Is it available?" is `containsKey`, O(1) |
| Returned loans | `ArrayList<ReturnedLoan>` | Append-only history |

**Main insight:** choose a data structure whose shape matches the business rule, and a whole class of bugs disappears.

### 3. Time is passed in, not read from the clock
Methods take `LocalDate today` instead of calling `LocalDate.now()`. Behaviour becomes deterministic and testable ("pretend it's 20 October"). In Spring, you inject a `Clock` for the same reason.

### 4. Don't leak internal collections
`allBooks()` returns `Collections.unmodifiableCollection(books.values())`. If it returned the real `books.values()`, any caller could `clear()` the library.

### 5. Business-rule failures are unchecked, specific exceptions
`BookNotFound`, `MemberNotFound`, `BookUnavailable`, `LoanLimitReached`, `NotOnLoan`, `Duplicate`, all under one `LibraryException`. Callers can catch a specific type or catch `LibraryException` for all of them. Spring uses the same style, and in Week 3 these become HTTP responses.

### 6. Three layers
- **Model:** the records
- **Logic:** `Library`
- **UI:** `Main`

`Library` never calls `System.out` or reads input. That is why the same `Library` can later sit behind a REST API without changes.

## Java concepts used

### Records
- Compact immutable data class. Auto-generates the constructor, accessors (`name()`, not `getName()`), `equals`, `hashCode`, `toString`.
- **Compact constructor:** runs *before* the fields are assigned, so you can validate input and the record can never hold bad data.
- Records can have methods (like `Loan.daysOverdue`) but not extra instance fields.

### `java.time`
- `LocalDate` is immutable: `plusDays` returns a **new** object.
- `ChronoUnit.DAYS.between(a, b)` counts days.
- Never use the old `java.util.Date`.

### Useful `Library` details
- `putIfAbsent` returns the previous value (`null` if none), so a non-null result means a duplicate.
- `borrow` validates both IDs first, then checks availability, then the loan limit.
- `loansOf` streams over `activeLoans.values()`. That is O(active loans) per call, which is fine here. A real system would index loans by member or let a database do it.
- `Collectors.groupingBy(..., TreeMap::new, counting())` is the three-argument form that picks the map type, so genres come out sorted.
- `mostBorrowed` merges two streams with `Stream.concat`, groups by ID, then sorts with a hand-written comparator (ties broken by book ID).
- In `Main`, `today` is reassigned by option 8. That's fine because no lambda captures it (the effectively-final rule from Day 3).
- `Scanner` on `System.in` should not be closed.

## Demo output and how to verify it

Expected output of `LibraryDemo`:

```
Genres: {Fiction=3, History=1, Programming=3}
Search 'the': [The Alchemist, The Hobbit]
Asha borrows a 4th book -> Asha already has 3 books
Ravi borrows a taken book -> Already borrowed: Effective Java
Ravi borrows unknown book -> No such book: B999
Available: [Brave New World, The Hobbit]
Overdue on 2026-10-20:
  Clean Code (member 1, 5 day(s) late)
  Design Patterns (member 1, 5 day(s) late)
  The Alchemist (member 2, 5 day(s) late)
  Sapiens (member 2, 1 day(s) late)
Fine for B101: 25
Fine for B104: 25
Total fines: 50
Ravi's loans: [Sapiens due 2026-10-19, Effective Java due 2026-11-03]
Most borrowed:
  Effective Java x2
  Clean Code x1
  Design Patterns x1
```

Checking the numbers by hand:
- Loans on 1 Oct are due 15 Oct. On 20 Oct that is 5 days late, so 5 x ₹5 = **₹25**.
- Ravi borrowed Sapiens on 5 Oct, due 19 Oct, so **1 day late** on 20 Oct.
- Effective Java was borrowed twice (Asha, then Ravi), so it leads "most borrowed". The tie among the rest is broken alphabetically by ID.

### Interactive session to try
`1` list all, `2` then `hobbit`, `3` then `B107`, `1` to borrow, `3` then `B107`, `2` (expect `Error: Already borrowed: The Hobbit`), `3` then `B107`, `abc` (friendly number-format message), `8` then `20` to advance, `6` for overdue (expect `6 day(s) late, Rs 30`), `4` then `B107` (fine Rs 30), `7` for stats.

## Things I broke on purpose

| Experiment | What happened | Lesson |
|------------|---------------|--------|
| `allBooks()` returns `books.values()` and the caller runs `.clear()` | The library was emptied from outside | Return an unmodifiable view. With it, the caller gets `UnsupportedOperationException`. |
| `new Book("", ...)` in `SeedData` | `IllegalArgumentException` from the compact constructor | Records validate before fields are assigned, so bad data can't exist |
| `returnAllFor` removing from `activeLoans` inside a for-each over `activeLoans.values()` | `ConcurrentModificationException` | Loop over a snapshot: `new ArrayList<>(loansOf(memberId))` (or use an iterator) |
| Removed `catch (NumberFormatException e)` and typed `abc` | Program crashed with a stack trace | Read stack traces from the top line down. Handle bad user input. |

## Extensions to consider

- **Waitlist:** `Map<String, Queue<Integer>>` so members can reserve a borrowed book.
- **Renew:** `Loan` is a record, so create a new one instead of mutating, and track that it was renewed.
- **Composable search:** build a `Predicate<Book>` from optional filters and combine with `and()`.
- **Export report:** write overdue loans to `overdue.txt` with `BufferedWriter` in try-with-resources.
- **Money:** don't use `double` for money. Use `BigDecimal`, or store the smallest currency unit as an integer (paise) to avoid rounding errors.

## DSA: hashing

### Hashing in 60 seconds
`HashMap` and `HashSet` give O(1) average lookup, insert and delete. The price is extra memory ("trade space for time", like Two Sum).

| Signal in the problem | Use |
|-----------------------|-----|
| "Have I seen this before?" | `HashSet` |
| "How many times does X occur?" | `HashMap<X, Integer>` |
| "Group items sharing a property" | `HashMap<Property, List<Item>>` |
| "Find a complement or pair" | `HashMap` of what you've seen |

**Key rule:** keys need consistent `equals` and `hashCode`. Strings, wrappers, records and `List<Integer>` are fine. **Arrays are not**: an `int[]` uses identity hashing, so two arrays with the same contents are different keys.

### Group Anagrams (LeetCode 49)
- **Key idea:** find one value that is identical for all anagrams of a word, and use it as the map key.
- **Idea A:** sorted word as the key (`eat`, `tea`, `ate` all become `aet`). O(n x k log k).
- **Idea B:** letter counts as the key, converted with `Arrays.toString(count)`. O(n x k).
- Both use O(n x k) space.
- **Why `Arrays.toString`:** an `int[]` as a key never groups anything, because every array is a different object. A `String` has content-based `equals`.
- **Edge cases:** empty string, single word, no anagrams, all identical words.
- **Unicode follow-up:** Idea A still works. Idea B needs a `HashMap<Character, Integer>`-based key.

### Longest Consecutive Sequence (LeetCode 128)
- **Simple idea:** sort and scan, O(n log n). Correct, but too slow for the O(n) requirement.
- **Better:** put everything in a `HashSet`. A number `n` starts a run only if `n - 1` is **not** in the set. From each start, walk `n + 1`, `n + 2`, ...
- **Why it is O(n) despite the nested loop:** the inner `while` only runs from run starts, and each number is walked at most once across all runs, so total work is about 2n, not n².
- **Edge cases:** empty array (0), single element, duplicates `[0, 0]` (1), negatives `[-1, 0, 1]` (3), full run `[5, 4, 3, 2, 1]` (5).

### Bonus
- Contains Duplicate (LeetCode 217): a one-liner with `HashSet`.
- Subarray Sum Equals K (LeetCode 560): prefix sums plus a `HashMap`, the most valuable hashing pattern in interviews.

## Self-check answers

1. **Why is `Book` immutable and the loan state kept separately?** Book data is a fact that doesn't change. Keeping availability in one separate place avoids two sources of truth that can disagree.
2. **Why does keying `activeLoans` by book ID enforce "one loan per book"?** A map key is unique, so a book can have at most one entry. `containsKey` answers "is it available?" in O(1).
3. **What does a compact constructor do, and when does it run?** It validates (or normalizes) the inputs, and it runs before the fields are assigned.
4. **Why does `borrow` take a `LocalDate`?** It makes behaviour deterministic and testable, and the same idea as injecting a `Clock` in Spring.
5. **What would go wrong if `allBooks()` returned `books.values()`?** Any caller could modify or `clear()` the library's internal map.
6. **Why did the return-all loop throw, and what are two fixes?** It removed entries from a map while iterating over it, causing `ConcurrentModificationException`. Fix by looping over a snapshot (`new ArrayList<>(...)`) or by using an iterator with `remove()`.
7. **How do you recognize a hashing problem?** Look for "seen before", counting, grouping, or finding a complement or pair.
8. **Why is Longest Consecutive Sequence O(n)?** The inner loop only starts at the beginning of a run, so each number is visited at most about twice overall.
9. **Why can't you use `int[]` directly as a `HashMap` key?** Arrays use identity-based `equals` and `hashCode`, so equal-content arrays are different keys. Convert to a `String` or a `List` first.

## One mistake I made today

> TODO: replace this with a real mistake from today (e.g. forgot to validate IDs before checking availability, mutated a map while looping over it, used an `int[]` as a map key, or closed the `Scanner`).