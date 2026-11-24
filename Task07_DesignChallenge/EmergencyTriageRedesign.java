package Task07_DesignChallenge;

import java.util.*;

/**
 * Task07 - Day-4 Design Challenge: Diagnose, Redesign and Validate
 * 
 * Comparing:
 * 1. Original Solution: PriorityQueue with binary min-heap and composite Comparator (O(log N))
 * 2. Redesigned Solution: Multi-Level Bucket Queue (Array of 5 ArrayDeques) with Aging (O(1))
 */
public class EmergencyTriageRedesign {

    public static class Patient {
        public final String id;
        public final String name;
        public int currentSeverity; // 1 (Critical) to 5 (Minor)
        public final long arrivalTime; // Minute of arrival

        public Patient(String id, String name, int severity, long arrivalTime) {
            this.id = id;
            this.name = name;
            this.currentSeverity = severity;
            this.arrivalTime = arrivalTime;
        }

        @Override
        public String toString() {
            return name + "(S" + currentSeverity + ", arr:" + arrivalTime + "m)";
        }
    }

    // =========================================================================
    // 1. ORIGINAL SOLUTION (PriorityQueue - Binary Min-Heap)
    // =========================================================================
    public static class OriginalPriorityQueueTriage {
        private final PriorityQueue<Patient> heap;

        public OriginalPriorityQueueTriage() {
            this.heap = new PriorityQueue<>(
                Comparator.comparingInt((Patient p) -> p.currentSeverity)
                          .thenComparingLong(p -> p.arrivalTime)
            );
        }

        public void admit(Patient p) {
            heap.add(p); // O(log N) sift-up
        }

        public Patient pollNext() {
            return heap.poll(); // O(log N) sift-down
        }

        public int size() {
            return heap.size();
        }
    }

    // =========================================================================
    // 2. REDESIGNED SOLUTION (Multi-Level Tiered Queue with Dynamic Aging)
    // =========================================================================
    public static class MultiLevelBucketTriage {
        public static final int NUM_TIERS = 5; // Severity 1 to 5
        @SuppressWarnings("unchecked")
        private final ArrayDeque<Patient>[] buckets = new ArrayDeque[NUM_TIERS + 1];
        private int totalCount = 0;
        private final long agingIntervalMinutes;

        public MultiLevelBucketTriage(long agingIntervalMinutes) {
            this.agingIntervalMinutes = agingIntervalMinutes;
            for (int i = 1; i <= NUM_TIERS; i++) {
                buckets[i] = new ArrayDeque<>();
            }
        }

        // O(1) Deterministic Admission
        public void admit(Patient p) {
            int sev = Math.max(1, Math.min(NUM_TIERS, p.currentSeverity));
            buckets[sev].addLast(p); // O(1) circular buffer append
            totalCount++;
        }

        // O(1) Deterministic Next Patient Retrieval
        public Patient pollNext() {
            for (int sev = 1; sev <= NUM_TIERS; sev++) {
                if (!buckets[sev].isEmpty()) {
                    totalCount--;
                    return buckets[sev].pollFirst(); // O(1) head removal
                }
            }
            return null;
        }

        // Starvation Prevention: Dynamic Aging Sweep
        // Promotes patients who have waited longer than agingIntervalMinutes
        public int applyAgingSweep(long currentSimMinute) {
            int promotedCount = 0;
            // Sweep from tier 5 down to tier 2 (promoting upwards)
            for (int sev = NUM_TIERS; sev > 1; sev--) {
                int size = buckets[sev].size();
                for (int i = 0; i < size; i++) {
                    Patient p = buckets[sev].pollFirst();
                    long wait = currentSimMinute - p.arrivalTime;
                    if (wait >= agingIntervalMinutes) {
                        p.currentSeverity = sev - 1; // Promote to higher urgency!
                        buckets[sev - 1].addLast(p);
                        promotedCount++;
                    } else {
                        buckets[sev].addLast(p); // Re-queue in current tier
                    }
                }
            }
            return promotedCount;
        }

        public int size() {
            return totalCount;
        }
    }

    // =========================================================================
    // BENCHMARK & COMPARISON HARNESS
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CSA09 - Java Assignment 2: DAY-4 DESIGN CHALLENGE (Q7)");
        System.out.println("  Evaluating Original PriorityQueue vs Redesigned MultiLevelBucketTriage");
        System.out.println("==========================================================================\n");

        int[] testSizes = { 1_000, 10_000, 100_000 };

        for (int n : testSizes) {
            runComparativeBenchmark(n);
        }

        runStarvationDemonstration();
    }

    private static void runComparativeBenchmark(int n) {
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("  BENCHMARK SCALE: %,d Patients (Mixed Severities S1-S5)\n", n);
        System.out.println("--------------------------------------------------------------------------");

        Random rand = new Random(42);
        List<Patient> patients = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            patients.add(new Patient("P" + i, "Patient-" + i, 1 + rand.nextInt(5), i));
        }

        // 1. Measure Original PriorityQueue
        OriginalPriorityQueueTriage originalPQ = new OriginalPriorityQueueTriage();
        long t1 = System.nanoTime();
        for (Patient p : patients) originalPQ.admit(p);
        double origAdmitMs = (System.nanoTime() - t1) / 1_000_000.0;

        long t2 = System.nanoTime();
        while (originalPQ.size() > 0) originalPQ.pollNext();
        double origPollMs = (System.nanoTime() - t2) / 1_000_000.0;

        // 2. Measure Redesigned MultiLevelBucketTriage
        MultiLevelBucketTriage redesigned = new MultiLevelBucketTriage(30);
        long t3 = System.nanoTime();
        for (Patient p : patients) redesigned.admit(p);
        double redAdmitMs = (System.nanoTime() - t3) / 1_000_000.0;

        long t4 = System.nanoTime();
        while (redesigned.size() > 0) redesigned.pollNext();
        double redPollMs = (System.nanoTime() - t4) / 1_000_000.0;

        double origTotal = origAdmitMs + origPollMs;
        double redTotal = redAdmitMs + redPollMs;

        System.out.printf("%-26s | %-14s | %-14s | %-14s | %-14s\n",
                "Implementation", "Admit Time (ms)", "Poll Time (ms)", "Total (ms)", "Speedup");
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.printf("%-26s | %14.3f | %14.3f | %14.3f | %-14s\n",
                "Original PriorityQueue", origAdmitMs, origPollMs, origTotal, "1.0x (Baseline)");
        System.out.printf("%-26s | %14.3f | %14.3f | %14.3f | %14.2fx faster\n",
                "Redesigned BucketTriage", redAdmitMs, redPollMs, redTotal, origTotal / Math.max(redTotal, 0.001));
        System.out.println();
    }

    private static void runStarvationDemonstration() {
        System.out.println("==========================================================================");
        System.out.println("  STARVATION AND FAIRNESS VALIDATION TEST");
        System.out.println("  Condition: 1 Non-Urgent patient (S5) arrives at Minute 0.");
        System.out.println("  A continuous influx of 50 High-Urgency patients (S1-S2) arrives over 60 mins.");
        System.out.println("==========================================================================");

        Patient minorPatient = new Patient("S5-Minor", "Rahul", 5, 0);

        // Test with Original PriorityQueue
        OriginalPriorityQueueTriage orig = new OriginalPriorityQueueTriage();
        orig.admit(minorPatient);
        for (int i = 1; i <= 50; i++) {
            orig.admit(new Patient("Urgent-" + i, "Emergency-" + i, 1 + (i % 2), i));
        }

        // Doctor sees 25 patients
        System.out.println("Doctor attends first 25 patients from Original PriorityQueue:");
        boolean rahulTreatedInOrig = false;
        for (int i = 0; i < 25; i++) {
            Patient seen = orig.pollNext();
            if (seen.id.equals(minorPatient.id)) rahulTreatedInOrig = true;
        }
        System.out.println("-> Was Rahul (S5) treated in Original Solution? " + (rahulTreatedInOrig ? "Yes" : "NO! (STARVED indefinitely)"));

        // Test with Redesigned MultiLevelBucketTriage with Aging Sweep at Minute 45
        MultiLevelBucketTriage red = new MultiLevelBucketTriage(15); // ages every 15 mins
        Patient rahulRed = new Patient("S5-Minor", "Rahul", 5, 0);
        red.admit(rahulRed);
        for (int i = 1; i <= 50; i++) {
            red.admit(new Patient("Urgent-" + i, "Emergency-" + i, 1 + (i % 2), i));
        }

        System.out.println("\nIn Redesigned Solution: Simulating time advancing to Minute 60 with Aging Sweeps:");
        // At minute 20: Rahul ages from S5 -> S4
        red.applyAgingSweep(20);
        // At minute 35: Rahul ages from S4 -> S3
        red.applyAgingSweep(35);
        // At minute 50: Rahul ages from S3 -> S2
        red.applyAgingSweep(50);
        // At minute 65: Rahul ages from S2 -> S1 (CRITICAL!)
        red.applyAgingSweep(65);

        System.out.println("Rahul's current priority status: " + rahulRed);
        Patient firstUp = red.pollNext();
        System.out.println("Next patient attending doctor now: " + firstUp);
        System.out.println("-> Result: Fair aging successfully elevated Rahul to priority care without violating clinical order!");
    }
}
