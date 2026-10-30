package com.ygtech.java_fullstack_journey.java_basics.day06;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Extension: write the overdue report to a text file. */
public final class ReportExporter {
    private ReportExporter() {}

    /** Returns the number of overdue loans written. Throws IOException if the file can't be written. */
    public static int writeOverdueReport(LibraryPlus lib, LocalDate today, Path file) throws IOException {
        List<Loan> overdue = lib.overdueLoans(today);

        // try-with-resources: the writer is closed (and flushed) even if something throws
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.write("Overdue report for " + today);
            out.newLine();
            out.write("-----------------------------");
            out.newLine();
            if (overdue.isEmpty()) {
                out.write("Nothing is overdue");
                out.newLine();
            }
            for (Loan l : overdue) {
                int days = l.daysOverdue(today);
                out.write(lib.getBook(l.isbn()).title()
                        + " | member " + l.memberId()
                        + " | " + days + " day(s) late"
                        + " | fine so far Rs " + days * LibraryPlus.FINE_PER_DAY);
                out.newLine();
            }
        }
        return overdue.size();
    }
}