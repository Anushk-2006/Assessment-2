package Task01;

import java.util.ArrayList;
import java.util.List;

/**
 * Task01: Building Text Under Load: String, StringBuilder or StringBuffer?
 * Problem Context: Railway e-Ticket Generation
 * 
 * Compares:
 * 1. String concatenation using '+' operator inside loops
 * 2. StringBuilder (unsynchronized, fast expandable buffer)
 * 3. StringBuffer (thread-safe, synchronized expandable buffer)
 */
public class TicketBenchmark {

    // Sample fields used to build railway e-tickets
    private static final String[] FIELD_TEMPLATES = {
        "PNR: 8429103847", "Train: 12626/Kerala Express", "From: NDLS", "To: TVC",
        "Class: 3A", "Quota: General", "Date: 28-Nov-2026", "Departure: 20:10",
        "Passenger: Arun Kumar, Age: 28, Gender: M, Coach: B3, Berth: 42 (MB)",
        "Passenger: Priya Nair, Age: 26, Gender: F, Coach: B3, Berth: 43 (UB)",
        "Base Fare: Rs. 3850", "Superfast Surcharge: Rs. 90", "Reservation Fee: Rs. 80",
        "GST: Rs. 201", "Total Fare: Rs. 4221", "Payment Mode: UPI/IRCTC-iMudra",
        "Status: CONFIRMED", "Transaction ID: TXN994820148", "Boarding Point: New Delhi",
        "Insurance: Opted (Rs. 0.90)", "Food Choice: Vegetarian Meal"
    };

    private static List<String> generateFields(int count) {
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(FIELD_TEMPLATES[i % FIELD_TEMPLATES.length] + " [Field #" + (i + 1) + "]\n");
        }
        return list;
    }

    // Method 1: String concatenation with '+'
    public static String buildTicketWithString(List<String> fields) {
        String ticket = "";
        for (String field : fields) {
            ticket = ticket + field;
        }
        return ticket;
    }

    // Method 2: StringBuilder
    public static String buildTicketWithStringBuilder(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            sb.append(field);
        }
        return sb.toString();
    }

    // Method 3: StringBuffer
    public static String buildTicketWithStringsBuffer(List<String> fields) {
        StringBuffer sb = new StringBuffer();
        for (String field : fields) {
            sb.append(field);
        }
        return sb.toString();
    }

    // Benchmark runner for a specific workload
    public static void runWorkload(String workloadName, int ticketCount, int fieldsPerTicket, int runs) {
        System.out.println("==========================================================================");
        System.out.printf("WORKLOAD: %s (%d tickets, %d fields/ticket, %d runs averaged)\n",
                workloadName, ticketCount, fieldsPerTicket, runs);
        System.out.println("==========================================================================");

        List<String> fields = generateFields(fieldsPerTicket);

        // Warmup JIT
        for (int i = 0; i < 50; i++) {
            buildTicketWithStringBuilder(fields);
            buildTicketWithStringsBuffer(fields);
            if (fieldsPerTicket <= 50) {
                buildTicketWithString(fields);
            }
        }

        // 1. Measure String (+)
        long totalTimeString = 0;
        long memBeforeString = getUsedMemory();
        long estIntermediateStrings = (long) ticketCount * fieldsPerTicket; // each loop iteration creates a new String
        for (int r = 0; r < runs; r++) {
            long start = System.nanoTime();
            for (int t = 0; t < ticketCount; t++) {
                String res = buildTicketWithString(fields);
            }
            totalTimeString += (System.nanoTime() - start);
        }
        long memAfterString = getUsedMemory();
        double avgTimeStringMs = (totalTimeString / (double) runs) / 1_000_000.0;

        // 2. Measure StringBuilder
        long totalTimeSB = 0;
        long memBeforeSB = getUsedMemory();
        for (int r = 0; r < runs; r++) {
            long start = System.nanoTime();
            for (int t = 0; t < ticketCount; t++) {
                String res = buildTicketWithStringBuilder(fields);
            }
            totalTimeSB += (System.nanoTime() - start);
        }
        long memAfterSB = getUsedMemory();
        double avgTimeSBMs = (totalTimeSB / (double) runs) / 1_000_000.0;

        // 3. Measure StringBuffer
        long totalTimeSBuf = 0;
        long memBeforeSBuf = getUsedMemory();
        for (int r = 0; r < runs; r++) {
            long start = System.nanoTime();
            for (int t = 0; t < ticketCount; t++) {
                String res = buildTicketWithStringsBuffer(fields);
            }
            totalTimeSBuf += (System.nanoTime() - start);
        }
        long memAfterSBuf = getUsedMemory();
        double avgTimeSBufMs = (totalTimeSBuf / (double) runs) / 1_000_000.0;

        System.out.printf("%-18s | %-15s | %-20s | %-16s\n", "Approach", "Avg Time (ms)", "Est. Interm. Objects", "Memory Delta (KB)");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-18s | %15.3f | %20d | %16.2f\n", "String (+)", avgTimeStringMs, estIntermediateStrings, (memAfterString - memBeforeString) / 1024.0);
        System.out.printf("%-18s | %15.3f | %20d | %16.2f\n", "StringBuilder", avgTimeSBMs, 0, (memAfterSB - memBeforeSB) / 1024.0);
        System.out.printf("%-18s | %15.3f | %20d | %16.2f\n", "StringBuffer", avgTimeSBufMs, 0, (memAfterSBuf - memBeforeSBuf) / 1024.0);
        System.out.println();
    }

    private static long getUsedMemory() {
        System.gc();
        try { Thread.sleep(15); } catch (InterruptedException ignored) {}
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CSA09 - Java Programming Assignment 2 (CO2) - Task 01 Benchmark");
        System.out.println("  Evaluating String vs StringBuilder vs StringBuffer");
        System.out.println("==========================================================================\n");

        // Workload 1: Single short ticket (1 ticket, 10 fields)
        runWorkload("1. Short Ticket", 1, 10, 100);

        // Workload 2: Standard production batch (10,000 tickets with 50 fields each)
        runWorkload("2. Production Batch", 10_000, 50, 5);

        // Workload 3: Large single bulk manifest / audit block (1 ticket with 5,000 fields)
        runWorkload("3. Massive Single Manifest", 1, 5_000, 10);
    }
}
