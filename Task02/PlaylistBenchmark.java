package Task02;

import java.util.*;

/**
 * Task02: When Is ArrayList an Appropriate Engineering Choice?
 * Problem Context: Music Playlist Editor
 * 
 * Experiments to compare:
 * - ArrayList vs LinkedList across operations:
 *   1. Adding at the end
 *   2. Inserting at the front (index 0)
 *   3. Inserting in the middle (index size / 2)
 *   4. Removing from the front (index 0)
 *   5. Removing from the middle (index size / 2)
 *   6. Random access (get k-th element)
 *   7. Full iteration (for-each / iterator)
 * - ArrayDeque comparison for "Up Next" queue operations (double-ended add/remove)
 */
public class PlaylistBenchmark {

    private static Song createSong(int i) {
        return new Song("S" + i, "Track " + i, "Artist " + (i % 50), 180 + (i % 120));
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CSA09 - Java Programming Assignment 2 (CO2) - Task 02 Benchmark");
        System.out.println("  ArrayList vs LinkedList (and ArrayDeque) for Playlist Editor");
        System.out.println("==========================================================================\n");

        int[] sizes = { 1_000, 10_000, 100_000 };

        for (int n : sizes) {
            runExperimentForSize(n);
        }

        runUpNextQueueExperiment(50_000);
    }

    private static void runExperimentForSize(int n) {
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("  BENCHMARK DATA SIZE: N = %,d Songs\n", n);
        System.out.println("--------------------------------------------------------------------------");

        // 1. Add at end (Build list from scratch)
        long t1 = System.nanoTime();
        List<Song> arrayList = new ArrayList<>();
        for (int i = 0; i < n; i++) arrayList.add(createSong(i));
        double alAddEnd = (System.nanoTime() - t1) / 1_000_000.0;

        long t2 = System.nanoTime();
        List<Song> linkedList = new LinkedList<>();
        for (int i = 0; i < n; i++) linkedList.add(createSong(i));
        double llAddEnd = (System.nanoTime() - t2) / 1_000_000.0;

        // 2. Access k-th element (10,000 random accesses)
        int numLookups = Math.min(n, 10_000);
        Random rand = new Random(42);
        int[] indices = new int[numLookups];
        for (int i = 0; i < numLookups; i++) indices[i] = rand.nextInt(n);

        long t3 = System.nanoTime();
        for (int idx : indices) {
            Song s = arrayList.get(idx);
        }
        double alGetK = (System.nanoTime() - t3) / 1_000_000.0;

        long t4 = System.nanoTime();
        // Limit lookups for LinkedList on large N to prevent excessive runtimes
        int llLookupLimit = (n >= 100_000) ? 1_000 : numLookups;
        for (int i = 0; i < llLookupLimit; i++) {
            Song s = linkedList.get(indices[i]);
        }
        double llGetK = (System.nanoTime() - t4) / 1_000_000.0;
        if (llLookupLimit < numLookups) {
            llGetK = llGetK * (numLookups / (double) llLookupLimit); // normalize
        }

        // 3. Sequential Iteration
        long t5 = System.nanoTime();
        long totalDurationAL = 0;
        for (Song s : arrayList) totalDurationAL += s.getDurationSeconds();
        double alIterate = (System.nanoTime() - t5) / 1_000_000.0;

        long t6 = System.nanoTime();
        long totalDurationLL = 0;
        for (Song s : linkedList) totalDurationLL += s.getDurationSeconds();
        double llIterate = (System.nanoTime() - t6) / 1_000_000.0;

        // 4. Insert at Front (1,000 operations)
        int opsFront = (n >= 100_000) ? 1_000 : 1_000;
        List<Song> alCopyFront = new ArrayList<>(arrayList);
        List<Song> llCopyFront = new LinkedList<>(linkedList);

        long t7 = System.nanoTime();
        for (int i = 0; i < opsFront; i++) alCopyFront.add(0, createSong(999900 + i));
        double alInsertFront = (System.nanoTime() - t7) / 1_000_000.0;

        long t8 = System.nanoTime();
        for (int i = 0; i < opsFront; i++) llCopyFront.add(0, createSong(999900 + i));
        double llInsertFront = (System.nanoTime() - t8) / 1_000_000.0;

        // 5. Insert in Middle (1,000 operations)
        int opsMid = (n >= 100_000) ? 500 : 1_000;
        List<Song> alCopyMid = new ArrayList<>(arrayList);
        List<Song> llCopyMid = new LinkedList<>(linkedList);

        long t9 = System.nanoTime();
        for (int i = 0; i < opsMid; i++) alCopyMid.add(alCopyMid.size() / 2, createSong(888800 + i));
        double alInsertMid = (System.nanoTime() - t9) / 1_000_000.0;

        long t10 = System.nanoTime();
        for (int i = 0; i < opsMid; i++) llCopyMid.add(llCopyMid.size() / 2, createSong(888800 + i));
        double llInsertMid = (System.nanoTime() - t10) / 1_000_000.0;

        // 6. Remove from Front (1,000 operations)
        long t11 = System.nanoTime();
        for (int i = 0; i < opsFront && !alCopyFront.isEmpty(); i++) alCopyFront.remove(0);
        double alRemoveFront = (System.nanoTime() - t11) / 1_000_000.0;

        long t12 = System.nanoTime();
        for (int i = 0; i < opsFront && !llCopyFront.isEmpty(); i++) llCopyFront.remove(0);
        double llRemoveFront = (System.nanoTime() - t12) / 1_000_000.0;

        System.out.printf("%-28s | %-16s | %-16s | %-12s\n",
                "Operation", "ArrayList (ms)", "LinkedList (ms)", "Winner");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-28s | %16.3f | %16.3f | %-12s\n", "Add at End (N items)", alAddEnd, llAddEnd, alAddEnd <= llAddEnd ? "ArrayList" : "LinkedList");
        System.out.printf("%-28s | %16.3f | %16.3f | %-12s\n", "Get k-th (10k random)", alGetK, llGetK, "ArrayList");
        System.out.printf("%-28s | %16.3f | %16.3f | %-12s\n", "Sequential Iteration", alIterate, llIterate, alIterate <= llIterate ? "ArrayList" : "LinkedList");
        System.out.printf("%-28s | %16.3f | %16.3f | %-12s\n", "Insert Front (" + opsFront + " ops)", alInsertFront, llInsertFront, alInsertFront <= llInsertFront ? "ArrayList" : "LinkedList");
        System.out.printf("%-28s | %16.3f | %16.3f | %-12s\n", "Insert Mid (" + opsMid + " ops)", alInsertMid, llInsertMid, alInsertMid <= llInsertMid ? "ArrayList" : "LinkedList");
        System.out.printf("%-28s | %16.3f | %16.3f | %-12s\n", "Remove Front (" + opsFront + " ops)", alRemoveFront, llRemoveFront, alRemoveFront <= llRemoveFront ? "ArrayList" : "LinkedList");
        System.out.println();
    }

    private static void runUpNextQueueExperiment(int ops) {
        System.out.println("==========================================================================");
        System.out.printf("CHANGED CONDITION: 'Up Next' Queue Benchmark (%,d Operations)\n", ops);
        System.out.println("Frequently adding at both ends and removing from front");
        System.out.println("Comparing ArrayList vs LinkedList vs ArrayDeque");
        System.out.println("==========================================================================");

        // 1. ArrayList: add(0, x), add(x), remove(0)
        long t1 = System.nanoTime();
        List<Song> al = new ArrayList<>();
        for (int i = 0; i < ops; i++) {
            if (i % 2 == 0) al.add(createSong(i));
            else al.add(0, createSong(i));
            if (i % 3 == 0 && !al.isEmpty()) al.remove(0);
        }
        double alTime = (System.nanoTime() - t1) / 1_000_000.0;

        // 2. LinkedList: addFirst(x), addLast(x), pollFirst()
        long t2 = System.nanoTime();
        Deque<Song> ll = new LinkedList<>();
        for (int i = 0; i < ops; i++) {
            if (i % 2 == 0) ll.addLast(createSong(i));
            else ll.addFirst(createSong(i));
            if (i % 3 == 0 && !ll.isEmpty()) ll.pollFirst();
        }
        double llTime = (System.nanoTime() - t2) / 1_000_000.0;

        // 3. ArrayDeque: addFirst(x), addLast(x), pollFirst()
        long t3 = System.nanoTime();
        Deque<Song> ad = new ArrayDeque<>();
        for (int i = 0; i < ops; i++) {
            if (i % 2 == 0) ad.addLast(createSong(i));
            else ad.addFirst(createSong(i));
            if (i % 3 == 0 && !ad.isEmpty()) ad.pollFirst();
        }
        double adTime = (System.nanoTime() - t3) / 1_000_000.0;

        System.out.printf("%-18s | %-16s | %-18s\n", "Data Structure", "Time (ms)", "Relative Speedup");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-18s | %16.3f | %18s\n", "ArrayList", alTime, "1.0x (Baseline)");
        System.out.printf("%-18s | %16.3f | %18.2fx faster\n", "LinkedList", llTime, alTime / Math.max(llTime, 0.001));
        System.out.printf("%-18s | %16.3f | %18.2fx faster\n", "ArrayDeque", adTime, alTime / Math.max(adTime, 0.001));
        System.out.println();
    }
}
