package Task03;

import java.util.*;

/**
 * Task03: Deep Investigation of hashCode() - equals() Contract
 * 
 * Tests designed:
 * Test 1: Set of scans where HashSet appears to work correctly without overriding hashCode()
 *         (Passing the identical object reference multiple times).
 * Test 2: Set of scans deliberately exposing duplicate entries when hashCode() is not overridden
 *         (Different object instances having identical regNo).
 * Test 3: Additional tests:
 *         (a) Modifying a hash-dependent key field after insertion into a HashSet.
 *         (b) TreeSet where compareTo() is inconsistent with equals().
 */
public class HashContractExperiment {

    // Faulty class without hashCode()
    static class BrokenStudent {
        String regNo;
        String name;

        BrokenStudent(String regNo, String name) {
            this.regNo = regNo;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrokenStudent)) return false;
            return Objects.equals(this.regNo, ((BrokenStudent) o).regNo);
        }
        // hashCode() intentionally omitted
        @Override
        public String toString() { return regNo + " (" + name + ")"; }
    }

    // Mutable student to test field modification post-insertion
    static class MutableStudent {
        String regNo;
        String name;

        MutableStudent(String regNo, String name) {
            this.regNo = regNo;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MutableStudent)) return false;
            return Objects.equals(this.regNo, ((MutableStudent) o).regNo);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(regNo);
        }

        @Override
        public String toString() { return regNo + " (" + name + ")"; }
    }

    // Student with compareTo inconsistent with equals
    static class InconsistentStudent implements Comparable<InconsistentStudent> {
        String regNo;
        String name;

        InconsistentStudent(String regNo, String name) {
            this.regNo = regNo;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InconsistentStudent)) return false;
            return Objects.equals(this.regNo, ((InconsistentStudent) o).regNo);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(regNo);
        }

        // Inconsistent: compares by name length/name instead of regNo!
        @Override
        public int compareTo(InconsistentStudent o) {
            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() { return regNo + " (" + name + ")"; }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  Task 03 - hashCode() and equals() Contract Investigation");
        System.out.println("==========================================================================\n");

        // -------------------------------------------------------------
        // Test 1: Where HashSet appears to work correctly
        // -------------------------------------------------------------
        System.out.println("--- Test 1: Scans where HashSet appears to work correctly ---");
        System.out.println("Prediction: Size will be 1, because the exact same object reference is scanned.");
        Set<BrokenStudent> test1Set = new HashSet<>();
        BrokenStudent arunCard = new BrokenStudent("192311001", "Arun");
        test1Set.add(arunCard);
        test1Set.add(arunCard); // Scanned again at Gate 2 using the same memory reference
        System.out.println("Observed Set Size: " + test1Set.size());
        System.out.println("Reason: HashSet checks 'e.hash == hash && ((k = e.key) == key || key.equals(k))'.");
        System.out.println("Because references are identical (arunCard == arunCard), identity check succeeds even with System.identityHashCode!\n");

        // -------------------------------------------------------------
        // Test 2: Deliberately exposed duplicates
        // -------------------------------------------------------------
        System.out.println("--- Test 2: Scans deliberately designed to expose duplicate entries ---");
        System.out.println("Prediction: Size will be 3 (expected 2), because two different instances with same regNo get different default hashCodes.");
        Set<BrokenStudent> test2Set = new HashSet<>();
        test2Set.add(new BrokenStudent("192311001", "Arun"));
        test2Set.add(new BrokenStudent("192311002", "Divya"));
        test2Set.add(new BrokenStudent("192311001", "Arun")); // Duplicate student, distinct heap instance
        System.out.println("Observed Set Size: " + test2Set.size());
        System.out.println("Contents: " + test2Set);
        System.out.println("Reason: Default Object.hashCode() uses identity/memory address hash. Two distinct instances hash to different buckets, so equals() is never even evaluated!\n");

        // -------------------------------------------------------------
        // Test 3A: Mutating a key field after insertion
        // -------------------------------------------------------------
        System.out.println("--- Test 3A: Mutating regNo after insertion into HashSet ---");
        Set<MutableStudent> test3ASet = new HashSet<>();
        MutableStudent s1 = new MutableStudent("192311001", "Arun");
        test3ASet.add(s1);
        System.out.println("Initial contains('192311001'): " + test3ASet.contains(s1));
        s1.regNo = "192311999"; // RegNo mutated!
        System.out.println("After mutating regNo to '192311999':");
        System.out.println("contains(s1): " + test3ASet.contains(s1)); // Returns false!
        System.out.println("Set size: " + test3ASet.size() + ", Elements: " + test3ASet);
        System.out.println("Reason: Object resides in bucket calculated from old hash. Lookup searches new hash bucket and finds nothing!\n");

        // -------------------------------------------------------------
        // Test 3B: TreeSet with compareTo inconsistent with equals
        // -------------------------------------------------------------
        System.out.println("--- Test 3B: TreeSet whose compareTo() is inconsistent with equals() ---");
        Set<InconsistentStudent> treeSet = new TreeSet<>();
        InconsistentStudent stA = new InconsistentStudent("192311001", "Kavya");
        InconsistentStudent stB = new InconsistentStudent("192311050", "Kavya"); // Different regNo, but same name!
        treeSet.add(stA);
        treeSet.add(stB);
        System.out.println("Prediction for 2 distinct students with same name: Size will erroneously be 1 in TreeSet!");
        System.out.println("Observed TreeSet Size: " + treeSet.size());
        System.out.println("Reason: TreeSet relies solely on compareTo() == 0, completely ignoring equals().");
    }
}
