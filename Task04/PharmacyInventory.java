package Task04;

import java.util.*;

/**
 * Task04: Choosing a Map for a Pharmacy Inventory
 * Problem Context: Hospital Pharmacy Stock Lookup
 * 
 * Compares HashMap, TreeMap, and LinkedHashMap across:
 * 1. 100,000 lookups
 * 2. Report 1 generation: Medicines in Code Order (sorted)
 * 3. Report 2 generation: Medicines in Insertion Order (as added)
 * 4. Verification of output order correctness and extra work needed
 */
public class PharmacyInventory {

    private static Medicine createMedicine(int id) {
        String code = String.format("MED-%06d", id);
        return new Medicine(code, "DrugName-" + id, 100 + (id % 500), 15.50 + (id % 200));
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CSA09 - Java Programming Assignment 2 (CO2) - Task 04 Benchmark");
        System.out.println("  Hospital Pharmacy Stock: HashMap vs TreeMap vs LinkedHashMap");
        System.out.println("==========================================================================\n");

        int[] inventorySizes = { 1_000, 10_000, 100_000 };

        for (int size : inventorySizes) {
            runInventoryBenchmark(size);
        }
    }

    private static void runInventoryBenchmark(int size) {
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("  INVENTORY DATASET SIZE: %,d Unique Medicines\n", size);
        System.out.println("--------------------------------------------------------------------------");

        // Prepare randomized insertion order
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(i);
        Collections.shuffle(ids, new Random(101));

        List<Medicine> medicineList = new ArrayList<>(size);
        for (int id : ids) {
            medicineList.add(createMedicine(id));
        }

        // 1. Populate Maps and measure insertion time
        Map<String, Medicine> hashMap = new HashMap<>();
        Map<String, Medicine> treeMap = new TreeMap<>();
        Map<String, Medicine> linkedHashMap = new LinkedHashMap<>();

        long t0 = System.nanoTime();
        for (Medicine m : medicineList) hashMap.put(m.getCode(), m);
        double hmPutTime = (System.nanoTime() - t0) / 1_000_000.0;

        long t1 = System.nanoTime();
        for (Medicine m : medicineList) treeMap.put(m.getCode(), m);
        double tmPutTime = (System.nanoTime() - t1) / 1_000_000.0;

        long t2 = System.nanoTime();
        for (Medicine m : medicineList) linkedHashMap.put(m.getCode(), m);
        double lhmPutTime = (System.nanoTime() - t2) / 1_000_000.0;

        // 2. Perform 100,000 lookups
        int lookupCount = 100_000;
        String[] queryCodes = new String[lookupCount];
        Random rand = new Random(42);
        for (int i = 0; i < lookupCount; i++) {
            queryCodes[i] = String.format("MED-%06d", rand.nextInt(size));
        }

        long t3 = System.nanoTime();
        for (String q : queryCodes) {
            Medicine m = hashMap.get(q);
        }
        double hmLookupTime = (System.nanoTime() - t3) / 1_000_000.0;

        long t4 = System.nanoTime();
        for (String q : queryCodes) {
            Medicine m = treeMap.get(q);
        }
        double tmLookupTime = (System.nanoTime() - t4) / 1_000_000.0;

        long t5 = System.nanoTime();
        for (String q : queryCodes) {
            Medicine m = linkedHashMap.get(q);
        }
        double lhmLookupTime = (System.nanoTime() - t5) / 1_000_000.0;

        // 3. Report 1: Medicines in Code Order (Natural sorted order)
        // TreeMap: Natural iterator
        long t6 = System.nanoTime();
        List<Medicine> r1_tm = new ArrayList<>(treeMap.values());
        double tmReport1Time = (System.nanoTime() - t6) / 1_000_000.0;
        boolean tmR1Sorted = isSortedByCode(r1_tm);

        // HashMap: Needs extra sorting!
        long t7 = System.nanoTime();
        List<Medicine> r1_hm = new ArrayList<>(hashMap.values());
        r1_hm.sort(Comparator.comparing(Medicine::getCode));
        double hmReport1Time = (System.nanoTime() - t7) / 1_000_000.0;
        boolean hmR1Sorted = isSortedByCode(r1_hm);

        // LinkedHashMap: Needs extra sorting!
        long t8 = System.nanoTime();
        List<Medicine> r1_lhm = new ArrayList<>(linkedHashMap.values());
        r1_lhm.sort(Comparator.comparing(Medicine::getCode));
        double lhmReport1Time = (System.nanoTime() - t8) / 1_000_000.0;
        boolean lhmR1Sorted = isSortedByCode(r1_lhm);

        // 4. Report 2: Medicines in Insertion Order (as added)
        // LinkedHashMap: Natural iterator
        long t9 = System.nanoTime();
        List<Medicine> r2_lhm = new ArrayList<>(linkedHashMap.values());
        double lhmReport2Time = (System.nanoTime() - t9) / 1_000_000.0;
        boolean lhmR2Matches = matchesOrder(r2_lhm, medicineList);

        // HashMap: Arbitrary hash bucket order, CANNOT reproduce insertion order without separate list!
        long t10 = System.nanoTime();
        List<Medicine> r2_hm = new ArrayList<>(hashMap.values());
        double hmReport2Time = (System.nanoTime() - t10) / 1_000_000.0;
        boolean hmR2Matches = matchesOrder(r2_hm, medicineList);

        // TreeMap: Keys sorted by code, CANNOT reproduce insertion order without separate list!
        long t11 = System.nanoTime();
        List<Medicine> r2_tm = new ArrayList<>(treeMap.values());
        double tmReport2Time = (System.nanoTime() - t11) / 1_000_000.0;
        boolean tmR2Matches = matchesOrder(r2_tm, medicineList);

        // Display results
        System.out.printf("%-16s | %-12s | %-14s | %-16s | %-16s\n",
                "Map Type", "Put (ms)", "100k Get (ms)", "Rep 1 (Code) ms", "Rep 2 (Insert) ms");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-16s | %12.3f | %14.3f | %16.3f | %16.3f\n",
                "HashMap", hmPutTime, hmLookupTime, hmReport1Time, hmReport2Time);
        System.out.printf("%-16s | %12.3f | %14.3f | %16.3f | %16.3f\n",
                "TreeMap", tmPutTime, tmLookupTime, tmReport1Time, tmReport2Time);
        System.out.printf("%-16s | %12.3f | %14.3f | %16.3f | %16.3f\n",
                "LinkedHashMap", lhmPutTime, lhmLookupTime, lhmReport1Time, lhmReport2Time);

        System.out.println("\nOrder Requirement Verification:");
        System.out.println("  Report 1 (Code Order naturally?): TreeMap=" + tmR1Sorted +
                ", HashMap=" + isSortedByCode(new ArrayList<>(hashMap.values())) + " (False without sort), LinkedHashMap=" + isSortedByCode(new ArrayList<>(linkedHashMap.values())) + " (False without sort)");
        System.out.println("  Report 2 (Insertion Order naturally?): LinkedHashMap=" + lhmR2Matches +
                ", HashMap=" + hmR2Matches + " (Fails!), TreeMap=" + tmR2Matches + " (Fails!)");
        System.out.println();
    }

    private static boolean isSortedByCode(List<Medicine> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).getCode().compareTo(list.get(i + 1).getCode()) > 0) return false;
        }
        return true;
    }

    private static boolean matchesOrder(List<Medicine> list1, List<Medicine> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).getCode().equals(list2.get(i).getCode())) return false;
        }
        return true;
    }
}
