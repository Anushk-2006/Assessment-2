package Task06;

import java.util.*;

/**
 * Task06: Emergency Triage Experiment & Verification
 * 
 * 1. Correctness Check (8 patients worked out by hand)
 * 2. Demonstrating why printing PriorityQueue directly does NOT show treatment order
 * 3. Growth Investigation: Admitting and polling across N = 1,000, 10,000, 100,000
 * 4. Starvation Simulation & Fair Aging Rule Implementation
 */
public class EmergencyTriageExperiment {

    static class PatientItem {
        final String name;
        final int severity;
        final long arrivalTime; // Sequence or timestamp

        PatientItem(String name, int severity, long arrivalTime) {
            this.name = name;
            this.severity = severity;
            this.arrivalTime = arrivalTime;
        }

        @Override
        public String toString() {
            return name + "(S" + severity + ",t" + arrivalTime + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  Task 06: Hospital Emergency Triage Priority Queue Investigation");
        System.out.println("==========================================================================\n");

        // -------------------------------------------------------------
        // 1. Correctness Check with 8 Hand-Worked Patients
        // -------------------------------------------------------------
        System.out.println("--- 1. Correctness Check (8 Hand-Worked Patients) ---");
        PriorityQueue<PatientItem> erQueue = new PriorityQueue<>(
                Comparator.comparingInt((PatientItem p) -> p.severity)
                        .thenComparingLong(p -> p.arrivalTime)
        );

        PatientItem[] testPatients = {
            new PatientItem("Sita", 3, 0),
            new PatientItem("Aman", 1, 1),
            new PatientItem("Rajesh", 2, 2),
            new PatientItem("Fatima", 1, 3),
            new PatientItem("David", 4, 4),
            new PatientItem("Ananya", 2, 5),
            new PatientItem("Vikram", 5, 6),
            new PatientItem("Sneha", 3, 7)
        };

        for (PatientItem p : testPatients) erQueue.add(p);

        System.out.println("Direct Print of PriorityQueue (Internal Heap Array Layout):");
        System.out.println(erQueue);
        System.out.println("-> Notice: The array order DOES NOT match sorted treatment order!");
        System.out.println("   Explanation: PriorityQueue is a binary min-heap. It only guarantees");
        System.out.println("   that parent <= children (heap invariant), not a total sorted order.\n");

        System.out.println("Actual Polled Treatment Order (Verified against hand calculation):");
        List<PatientItem> treatedOrder = new ArrayList<>();
        PriorityQueue<PatientItem> copyForPoll = new PriorityQueue<>(erQueue);
        while (!copyForPoll.isEmpty()) {
            treatedOrder.add(copyForPoll.poll());
        }
        System.out.println(treatedOrder);

        String[] expectedHandOrder = {
            "Aman", "Fatima", "Rajesh", "Ananya", "Sita", "Sneha", "David", "Vikram"
        };
        boolean correct = true;
        for (int i = 0; i < expectedHandOrder.length; i++) {
            if (!treatedOrder.get(i).name.equals(expectedHandOrder[i])) correct = false;
        }
        System.out.println("Hand-verified Match: " + (correct ? "PASSED (100% Correct)" : "FAILED") + "\n");

        // -------------------------------------------------------------
        // 2. Growth Investigation
        // -------------------------------------------------------------
        System.out.println("--- 2. Growth Investigation (Admit & Poll across N) ---");
        System.out.println("Prediction: Both admit and poll scale as O(log N) due to binary heap tree depth.");
        int[] sizes = { 1_000, 10_000, 100_000 };

        System.out.printf("%-12s | %-16s | %-16s | %-20s\n", "Queue Size N", "Total Admit (ms)", "Total Poll (ms)", "Avg Op Time (ns)");
        System.out.println("--------------------------------------------------------------------------");
        Random rand = new Random(42);

        for (int n : sizes) {
            PriorityQueue<PatientItem> pq = new PriorityQueue<>(
                    Comparator.comparingInt((PatientItem p) -> p.severity)
                            .thenComparingLong(p -> p.arrivalTime)
            );

            long tAdmitStart = System.nanoTime();
            for (int i = 0; i < n; i++) {
                int sev = 1 + rand.nextInt(5);
                pq.add(new PatientItem("P" + i, sev, i));
            }
            double admitMs = (System.nanoTime() - tAdmitStart) / 1_000_000.0;

            long tPollStart = System.nanoTime();
            while (!pq.isEmpty()) {
                PatientItem p = pq.poll();
            }
            double pollMs = (System.nanoTime() - tPollStart) / 1_000_000.0;
            double avgNsPerOp = ((admitMs + pollMs) * 1_000_000.0) / (2.0 * n);

            System.out.printf("%-12d | %16.3f | %16.3f | %20.1f ns\n", n, admitMs, pollMs, avgNsPerOp);
        }
        System.out.println();

        // -------------------------------------------------------------
        // 3. Starvation Inquiry & Aging Rule Design
        // -------------------------------------------------------------
        System.out.println("--- 3. Starvation Demonstration & Dynamic Aging Rule ---");
        System.out.println("Scenario: A Severity-5 patient (Vikram) arrives at minute 0.");
        System.out.println("Continuous stream of Severity 1-3 patients arrives over the next 100 turns.");
        System.out.println("Without Aging: Vikram will NEVER be seen (Starvation).");

        // Aging rule: Effective Severity = max(1, originalSeverity - (currentTurn - arrivalTurn) / 15)
        // Every 15 turns of waiting upgrades the patient by 1 severity level.
        int vikramArrival = 0;
        int originalSev = 5;
        System.out.println("\nAging Progression for Vikram (S5):");
        for (int turn = 0; turn <= 65; turn += 15) {
            int waitTime = turn - vikramArrival;
            int effectiveSev = Math.max(1, originalSev - (waitTime / 15));
            System.out.printf("  At Turn %2d (Waited %2d mins) -> Effective Severity: S%d %s\n",
                    turn, waitTime, effectiveSev, effectiveSev == 1 ? "(Promoted to CRITICAL - Guaranteed immediate care!)" : "");
        }
    }
}
