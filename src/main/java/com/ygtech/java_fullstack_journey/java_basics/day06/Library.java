package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Library {
    public static final int LOAN_DAYS = 14;
    public static final int MAX_LOANS = 3;
    public static final int FINE_PER_DAY = 5;   // rupees

    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<Integer, Member> members = new LinkedHashMap<>();
    private final Map<String, Loan> activeLoans = new HashMap<>();   // book id -> its one loan
    private final List<ReturnedLoan> history = new ArrayList<>();

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
        if (loansOf(memberId).size() >= MAX_LOANS) {
            throw new LibraryException.LoanLimitReached(member.name(), MAX_LOANS);
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
        int fine = loan.daysOverdue(today) * FINE_PER_DAY;
        history.add(new ReturnedLoan(loan, today, fine));
        return fine;
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