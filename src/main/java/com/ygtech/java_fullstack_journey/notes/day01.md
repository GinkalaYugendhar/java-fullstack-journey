# Java Full Stack Journey

## Day 1 Notes: Setup, OOP Revision, First Push

## The four OOP pillars (in my own words)

1. **Encapsulation**: Keep fields `private` and expose them only through methods that enforce rules. In my banking model, `balance` can only change via `deposit()` and `withdraw()`, so it can never be set to an invalid value directly.
2. **Inheritance**: A child class reuses and extends a parent with `extends`. `SavingsAccount` and `CurrentAccount` both inherit deposit/withdraw logic from `Account` and only define what differs.
3. **Polymorphism**: The same method call behaves differently depending on the actual object. `acc.withdraw(4500)` fails for Savings (min balance 1000) but succeeds for Current (overdraft up to -5000), even though `acc` is typed as `Account`.
4. **Abstraction**: Expose what can be done and hide how. `Notifier` is an interface, so `Account` only knows `send(message)`, not whether it goes out by email or SMS.

## Abstract class vs interface

- **Abstract class** (`Account`): subclasses share state (fields) and some behaviour.
- **Interface** (`Notifier`): a capability that unrelated classes can implement. A class can implement many interfaces but extend only one class.

## What I built

A mini banking model in `java-basics/day01/`:

| File | Role |
|------|------|
| `Notifier.java` | Interface with `send(String)` |
| `EmailNotifier.java`, `SmsNotifier.java` | Implementations of `Notifier` |
| `Account.java` | Abstract class, private fields, `deposit`/`withdraw` |
| `SavingsAccount.java`, `CurrentAccount.java` | Subclasses with different `minBalance()` |
| `Main.java` | Polymorphism demo with try/catch |

## Things I tried

- **try/catch in the loop**: Without it, the `IllegalStateException` for Savings crashed the program and the Current account never ran. With it, the loop continues.
- **Swapping notifiers**: Changing `new EmailNotifier()` to `new SmsNotifier()` in one line switched the output from `[EMAIL]` to `[SMS]`, and `Account` didn't change at all. This is the idea behind dependency injection in Spring.
- **Accessing `balance` directly**: Compiler error `balance has private access in day01.Account`. That's encapsulation working.

## Other takeaways

- `javac` compiles `.java` into bytecode. `java` runs it on the JVM.
- `super(...)` calls the parent class constructor.
- Real banking code uses `BigDecimal`, not `double`, because of floating-point rounding errors.

## DSA

**Second largest distinct element**: one pass with `first` and `second`. O(n) time, O(1) space. Edge cases: all equal, single element, negatives, duplicates of the max.

**Move zeroes (LeetCode 283)**: two pointers, `read` scans everything and `write` marks where the next non-zero goes. O(n) time, O(1) space, in place.

## Git commands used

```bash
git init
git add .
git commit -m "Day 1: setup, OOP banking model, README"
git branch -M main
git remote add origin https://github.com/GinkalaYugendhar/java-fullstack-journey.git
git push -u origin main
git add .
git commit -m "Day 1: second largest, move zeroes"
git push
```
