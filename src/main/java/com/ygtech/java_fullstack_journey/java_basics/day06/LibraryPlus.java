package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A copy of Library.java with two extensions added, so your original file stays untouched.
 * New parts are marked "EXTENSION". Once you're happy, merge them into Library.java if you like.
 *
 *  - Waitlist: a member can reserve a borrowed book; when it is returned we report who is next.
 *  - Renew:    a loan can be renewed once for 7 more days. Loan is a record, so we REPLACE it
 *              with a new Loan instead of changing it.
 */
public class LibraryPlus {
    public static final int LOAN_DAYS = 14;
    public static final int MAX_LOANS = 3;
    public static final int FINE_PER_DAY = 5;   // rupees
    public static final int RENEW_DAYS = 7;     // EXTENSION

    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<Integer, Member> members = new LinkedHashMap<>();
    private final Map<String, Loan> activeLoans = new HashMap<>();   // book id -> its one loan
    private final List<ReturnedLoan> history = new ArrayList<>();

    // EXTENSION: book id -> members waiting for it, first come first served
    private final Map<String, Queue<Integer>> waitlist = new HashMap<>();
    // EXTENSION: book ids whose CURRENT loan has already been renewed once
    private final Set<String> renewed = new HashSet<>();

    // ---------- commands (change state) ----------

    public void addBook(Book book) {
        if (books.putIfAbsent(book.isbn(), book) != null) {
            throw new LibraryException.Duplicate("Book already exists: " + book.isbn());
        }
    }

    public void addMember(Member member) {
        if (members.putIfAbsent(member.id(), member) != null) {
            throw new LibraryException.Duplicate("Member already exists: " + member.id());
        }
    }

    public Loan borrow(String isbn, int memberId, LocalDate today) {
        Book book = getBook(isbn);
        Member member = getMember(memberId);

        if (activeLoans.containsKey(isbn)) {
            throw new LibraryException.BookUnavailable(book.title());
        }
        // EXTENSION: if people are waiting, only the one at the front may borrow it
        Queue<Integer> queue = waitlist.get(isbn);
        if (queue != null && !queue.isEmpty() && queue.peek() != memberId) {
            throw new LibraryException("Reserved for member " + queue.peek() + ": " + book.title());
        }
        if (loansOf(memberId).size() >= MAX_LOANS) {
            throw new LibraryException.LoanLimitReached(member.name(), MAX_LOANS);
        }

        if (queue != null && !queue.isEmpty()) {
            queue.poll();   // EXTENSION: this member has now used their place in the queue
        }
        Loan loan = new Loan(isbn, memberId, today, today.plusDays(LOAN_DAYS));
        activeLoans.put(isbn, loan);
        return loan;
    }

    /** Returns the fine in rupees (0 if on time). */
    public int returnBook(String isbn, LocalDate today) {
        getBook(isbn);   // throws BookNotFound if the id is wrong
        Loan loan = activeLoans.remove(isbn);
        if (loan == null) {
            throw new LibraryException.NotOnLoan(isbn);
        }
        renewed.remove(isbn);   // EXTENSION: a new loan starts with a clean renewal state
        int fine = loan.daysOverdue(today) * FINE_PER_DAY;
        history.add(new ReturnedLoan(loan, today, fine));
        return fine;
    }

    // ---------- EXTENSION: waitlist ----------

    public void reserve(String isbn, int memberId) {
        Book book = getBook(isbn);
        getMember(memberId);

        Loan loan = activeLoans.get(isbn);
        if (loan == null) {
            throw new LibraryException("Book is available, borrow it directly: " + book.title());
        }
        if (loan.memberId() == memberId) {
            throw new LibraryException("Member " + memberId + " already has this book");
        }
        Queue<Integer> queue = waitlist.computeIfAbsent(isbn, k -> new ArrayDeque<>());
        if (queue.contains(memberId)) {
            throw new LibraryException.Duplicate("Member " + memberId + " is already waiting for " + book.title());
        }
        queue.offer(memberId);
    }

    /** Who is next in line for this book? (peek: does not remove them) */
    public Optional<Integer> nextInWaitlist(String isbn) {
        return Optional.ofNullable(waitlist.get(isbn)).map(Queue::peek);
    }

    public List<Integer> waitlistFor(String isbn) {
        return List.copyOf(waitlist.getOrDefault(isbn, new ArrayDeque<>()));
    }

    // ---------- EXTENSION: renew ----------

    public Loan renew(String isbn, LocalDate today) {
        getBook(isbn);
        Loan loan = activeLoans.get(isbn);
        if (loan == null) {
            throw new LibraryException.NotOnLoan(isbn);
        }
        if (renewed.contains(isbn)) {
            throw new LibraryException("Already renewed once: " + isbn);
        }
        if (loan.isOverdue(today)) {
            throw new LibraryException("Cannot renew an overdue book: " + isbn);
        }
        if (nextInWaitlist(isbn).isPresent()) {
            throw new LibraryException("Cannot renew, other members are waiting: " + isbn);
        }
        // Loan is a record (immutable): build a new one with a later due date and swap it in
        Loan renewedLoan = new Loan(loan.isbn(), loan.memberId(), loan.borrowedOn(), loan.dueOn().plusDays(RENEW_DAYS));
        activeLoans.put(isbn, renewedLoan);
        renewed.add(isbn);
        return renewedLoan;
    }

    // ---------- lookups ----------

    public Book getBook(String isbn) {
        return Optional.ofNullable(books.get(isbn))
                .orElseThrow(() -> new LibraryException.BookNotFound(isbn));
    }

    public Member getMember(int id) {
        return Optional.ofNullable(members.get(id))
                .orElseThrow(() -> new LibraryException.MemberNotFound(id));
    }

    public Optional<Loan> loanFor(String isbn) {
        return Optional.ofNullable(activeLoans.get(isbn));
    }

    /** Read-only view: callers cannot modify the library through it. */
    public Collection<Book> allBooks() {
        return Collections.unmodifiableCollection(books.values());
    }

    // ---------- queries (streams) ----------

    public List<Book> search(String text) {
        String q = text.toLowerCase();
        return books.values().stream()
                .filter(b -> b.title().toLowerCase().contains(q)
                        || b.author().toLowerCase().contains(q))
                .toList();
    }

    public List<Book> availableBooks() {
        return books.values().stream()
                .filter(b -> !activeLoans.containsKey(b.isbn()))
                .toList();
    }

    public List<Loan> loansOf(int memberId) {
        return activeLoans.values().stream()
                .filter(l -> l.memberId() == memberId)
                .sorted(Comparator.comparing(Loan::dueOn).thenComparing(Loan::isbn))
                .toList();
    }

    public List<Loan> overdueLoans(LocalDate today) {
        return activeLoans.values().stream()
                .filter(l -> l.isOverdue(today))
                .sorted(Comparator.comparing(Loan::dueOn).thenComparing(Loan::isbn))
                .toList();
    }

    public Map<String, Long> countByGenre() {
        return books.values().stream()
                .collect(Collectors.groupingBy(Book::genre, TreeMap::new, Collectors.counting()));
    }

    /** Counts every borrow, past and current. Ties are broken by book id. */
    public List<Map.Entry<String, Long>> mostBorrowed(int n) {
        Map<String, Long> counts = Stream.concat(
                        history.stream().map(r -> r.loan().isbn()),
                        activeLoans.keySet().stream())
                .collect(Collectors.groupingBy(isbn -> isbn, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(n)
                .toList();
    }

    public int totalFinesCollected() {
        return history.stream().mapToInt(ReturnedLoan::fine).sum();
    }
}