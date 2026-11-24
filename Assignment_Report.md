# CSA09 – PROGRAMMING IN JAVA – ASSIGNMENT 2 (CO2)
## COMPREHENSIVE LABORATORY REPORT & EXPERIMENTAL DEFENSE

**Course:** CSA09 – Programming in Java  
**Topic Coverage:** Strings and StringBuilder, regular expressions, Collection Framework (List, Queue, Deque, PriorityQueue, Set, Map), hashCode–equals contract, Generics, Comparable and Comparator, and Collections utility class (Sessions 15–26).  
**Student Name:** Anushk Ramesh  
**GitHub Username:** [Anushk-2006](https://github.com/Anushk-2006)  
**Register Number:** `<Register-Number>`  
**Environment:** OpenJDK 21.0.12.1 LTS (64-Bit Server VM, Microsoft Build 21.0.12.1+1-LTS, Windows x64)  
**GitHub Submission Repository:** [https://github.com/Anushk-2006/Assessment-2.git](https://github.com/Anushk-2006/Assessment-2.git)

---

# TABLE OF CONTENTS
1. [Task 01 – Building Text Under Load: String, StringBuilder or StringBuffer?](#task-01--building-text-under-load-string-stringbuilder-or-stringbuffer)
2. [Task 02 – When Is ArrayList an Appropriate Engineering Choice?](#task-02--when-is-arraylist-an-appropriate-engineering-choice)
3. [Task 03 – Engineer the hashCode–equals Contract](#task-03--engineer-the-hashcodeequals-contract)
4. [Task 04 – Choosing a Map for a Pharmacy Inventory](#task-04--choosing-a-map-for-a-pharmacy-inventory)
5. [Task 05 – Derive a Generic Ranking Component from a Principle](#task-05--derive-a-generic-ranking-component-from-a-principle)
6. [Task 06 – Priority, Order and Fairness in a Queue](#task-06--priority-order-and-fairness-in-a-queue)
7. [Task 07 – Day-4 Design Challenge: Diagnose, Redesign and Validate](#task-07--day-4-design-challenge-diagnose-redesign-and-validate)

---

# Task 01 – Building Text Under Load: String, StringBuilder or StringBuffer?
**Bloom's Level: K4 – Analyse | Marks: 6**

### Problem Context – Railway e-Ticket Generation
A railway reservation system generates an e-ticket for every booking by joining many fields (PNR, passenger names, coach, berth, fare, taxes, transaction details, journey details) into a single printable text block. The legacy code builds the ticket using the `+` operator inside a loop.

### 1. Predictions Before Implementation
* **String (`+` operator):** Because Java strings are immutable, each concatenation in a loop must allocate a new backing buffer and copy all previously accumulated characters. For $n$ concatenations of fields with average length $L$, the number of characters copied across the loop is:
  $$\text{Total Chars Copied} = \sum_{i=1}^{n} (i \cdot L) = L \cdot \frac{n(n+1)}{2} = \mathcal{O}(n^2 L)$$
  This will exhibit catastrophic quadratic time degradation and generate $n$ short-lived intermediate heap objects per ticket, causing Garbage Collection (GC) pauses.
* **StringBuilder:** Employs an internal mutable array (`byte[]` in Java 9+) that expands geometrically when full (typically $\text{newCapacity} = 2 \times \text{capacity} + 2$). Appends run in amortized $\mathcal{O}(1)$ time, yielding total time $\mathcal{O}(n L)$ and allocating zero intermediate strings.
* **StringBuffer:** Identical algorithmic complexity to `StringBuilder` ($\mathcal{O}(n L)$), but every method is marked `synchronized`. In a single-threaded loop, it incurs thread synchronization overhead (monitorenter/monitorexit), making it marginally slower than `StringBuilder`.

### 2. Contrasting Workload Design
1. **Workload 1 (Short Ticket):** 1 ticket with 10 fields (isolated unreserved ticket).
2. **Workload 2 (Production Tatkal Batch):** 10,000 tickets with 50 fields each (peak ticket booking load).
3. **Workload 3 (Massive Single Manifest):** 1 ticket/manifest with 5,000 fields (stress test exposing quadratic behavior).

### 3. Measured Evidence (Recorded on OpenJDK 21.0.12.1)

| Workload | Approach | Average Execution Time (ms) | Estimated Intermediate Objects | Memory Delta (KB) | Speedup vs `+` |
|---|---|---|---|---|---|
| **1. Short Ticket** (1 ticket, 10 fields) | String (`+`) | 0.009 ms | 10 | Baseline | 1.0x |
| | StringBuilder | 0.010 ms | 0 | 0.00 KB | ~1.0x |
| | StringBuffer | 0.006 ms | 0 | 0.00 KB | ~1.0x |
| **2. Production Batch** (10,000 tickets, 50 fields) | String (`+`) | 161.005 ms | 500,000 | +0.45 KB | 1.0x |
| | StringBuilder | 26.367 ms | 0 | +0.38 KB | **6.1x faster** |
| | StringBuffer | 23.351 ms | 0 | -4.09 KB | **6.9x faster** |
| **3. Massive Single Manifest** (1 ticket, 5,000 fields) | String (`+`) | 127.470 ms | 5,000 | 0.00 KB | 1.0x |
| | StringBuilder | 0.315 ms | 0 | 0.00 KB | **404.7x faster** |
| | StringBuffer | 0.382 ms | 0 | -422.68 KB | **333.7x faster** |

### 4. Comparison of Predicted vs Observed Behavior
* For small single tickets (Workload 1), the difference is sub-millisecond (< 0.01 ms) and practically negligible.
* For the 10,000 ticket batch (Workload 2), repeated concatenation created **500,000 intermediate String objects**, running 6x slower than `StringBuilder`.
* For the 5,000-field single ticket (Workload 3), quadratic character copying caused `+` to take **127.47 ms for just one document**, while `StringBuilder` completed in **0.31 ms (404 times faster)**, confirming theoretical derivations.

### 5. Design Decision: Multithreaded Booking Server
* **For Per-Ticket Text Generation:** **`StringBuilder`**. Tickets are constructed within a single worker thread's stack frame (thread-confined). Synchronization is redundant and adds pure overhead.
* **For Shared Booking Log:** **`StringBuffer` or a Concurrent Queue (`ConcurrentLinkedQueue<String>`)**. If multiple threads append directly to a shared text stream without external locks, `StringBuilder` experiences race conditions on its internal length counters and corrupts the buffer. `StringBuffer` provides thread-safe method-level synchronization. (In ultra-high-throughput architectures, asynchronous loggers using Disruptor ring buffers are preferred).

### 6. Alternative Concept & Complexity Analysis
* **`String.join()` & `StringJoiner`:** Built internally on `StringBuilder`. Ideal for comma-separated or delimited ticket fields with clean syntax and $\mathcal{O}(n L)$ time.
* **`String.format()`:** Parses format specifiers using regular expressions on every invocation and boxes primitive types, resulting in a 5x–15x performance penalty over direct `StringBuilder` appends.
* **Derivation Summary:**
  * Repeated `+`: $\mathcal{O}(n^2 L)$ characters copied, $\mathcal{O}(n)$ intermediate objects.
  * `StringBuilder`: $\mathcal{O}(n L)$ amortized time, 0 intermediate objects.

---

# Task 02 – When Is ArrayList an Appropriate Engineering Choice?
**Bloom's Level: K4 – Analyse | Marks: 4**

### Problem Context – Music Playlist Editor
A music-streaming app lets users add songs at the end, insert songs at chosen positions, remove songs, and jump directly to the $k$-th song. The app currently stores every playlist in an `ArrayList<Song>`.

### 1. Experimental Evidence Across 3 Playlist Sizes

| Playlist Scale | Operation | `ArrayList` (ms) | `LinkedList` (ms) | Winner |
|---|---|---|---|---|
| **$N = 1,000$** | Add at End ($N$ items) | 21.70 ms | 3.25 ms | LinkedList (JIT warmup artifact) |
| | **Get $k$-th (10k random lookups)** | **0.43 ms** | 2.88 ms | **ArrayList (6.7x faster)** |
| | Sequential Iteration | **1.14 ms** | 2.00 ms | ArrayList |
| | Insert at Front (1k ops) | 2.89 ms | **1.95 ms** | LinkedList |
| | Insert in Middle (1k ops) | **1.88 ms** | 6.50 ms | **ArrayList (3.5x faster)** |
| | Remove from Front (1k ops) | 1.93 ms | **0.75 ms** | LinkedList |
| **$N = 10,000$** | Add at End ($N$ items) | 7.26 ms | 4.47 ms | Comparable |
| | **Get $k$-th (10k random lookups)** | **1.71 ms** | **267.00 ms** | **ArrayList (156x faster)** |
| | Sequential Iteration | **0.52 ms** | 0.53 ms | Comparable |
| | Insert at Front (1k ops) | 2.34 ms | **0.33 ms** | LinkedList |
| | Insert in Middle (1k ops) | **1.10 ms** | 15.44 ms | **ArrayList (14x faster)** |
| | Remove from Front (1k ops) | 1.46 ms | **0.12 ms** | LinkedList |
| **$N = 100,000$** | Add at End ($N$ items) | **27.25 ms** | 39.74 ms | **ArrayList (avoids 100k Node allocs)** |
| | **Get $k$-th (10k random lookups)** | **2.37 ms** | **5,848.14 ms** | **ArrayList (2,467x faster!)** |
| | Sequential Iteration | **3.59 ms** | 5.62 ms | **ArrayList (cache line prefetching)** |
| | Insert at Front (1k ops) | 17.08 ms | **0.11 ms** | LinkedList |
| | Insert in Middle (500 ops) | **4.55 ms** | 73.01 ms | **ArrayList (16x faster)** |
| | Remove from Front (1k ops) | 14.01 ms | **0.07 ms** | LinkedList |

### 2. Engineering Decision Boundary
* **When `ArrayList` is the reasonable engineering choice:**
  1. **Random Access Playback (`get(k)`):** Direct pointer arithmetic `base + index * 4` delivers pure $\mathcal{O}(1)$ performance (2.37 ms for 10k lookups vs 5.85 seconds in `LinkedList`).
  2. **Sequential Traversal:** Contiguous array references leverage hardware L1/L2 cache line prefetching. `LinkedList` causes pointer-chasing cache misses across scattered heap addresses.
  3. **Middle Insertions at Moderate Sizes:** `System.arraycopy` memory block copying is faster on modern CPUs than walking $N/2$ linked nodes in `LinkedList`.
* **When to stop recommending `ArrayList`:**
  * When the dominant pattern is **continuous insertions or deletions at the head / front of large lists** ($\mathcal{O}(N)$ element shifting overhead).

### 3. Changed Condition: The "Up Next" Queue
* **Condition:** Frequent additions at both ends and removals from the front (50,000 operations).
* **Measured Times:**
  * `ArrayList`: **177.91 ms** (1.0x baseline, crippled by $O(N)$ front shifts).
  * `LinkedList`: **10.44 ms** (17.0x faster).
  * `ArrayDeque`: **10.03 ms** (**17.7x faster**, optimal winner).
* **Recommendation:** Deploy **`ArrayDeque`**. It implements a circular array with head and tail indices, offering amortized $\mathcal{O}(1)$ operations at both ends with **zero node allocation overhead** and compact memory density.

### 4. Complexity Comparison Table

| Operation | `ArrayList` | `LinkedList` | `ArrayDeque` | Plain Array with Manual Shifting |
|---|---|---|---|---|
| **Add at End** | Amortized $\mathcal{O}(1)$ | $\mathcal{O}(1)$ | Amortized $\mathcal{O}(1)$ | Amortized $\mathcal{O}(1)$ |
| **Insert at Front** | $\mathcal{O}(n)$ | $\mathcal{O}(1)$ | Amortized $\mathcal{O}(1)$ | $\mathcal{O}(n)$ |
| **Insert in Middle** | $\mathcal{O}(n)$ (fast copy) | $\mathcal{O}(n)$ (slow traverse) | Unsupported | $\mathcal{O}(n)$ |
| **Get $k$-th** | $\mathcal{O}(1)$ | $\mathcal{O}(n)$ | Unsupported by index | $\mathcal{O}(1)$ |
| **Iterate Full List** | $\mathcal{O}(n)$ (optimal cache) | $\mathcal{O}(n)$ (cache misses) | $\mathcal{O}(n)$ (optimal cache) | $\mathcal{O}(n)$ (optimal cache) |
| **Memory per Item** | **4–8 bytes** | **24–32 bytes** | **4–8 bytes** | **4–8 bytes** |

---

# Task 03 – Engineer the hashCode–equals Contract
**Bloom's Level: K4 – Analyse | Marks: 6**

### Problem Context – Event Attendance De-duplication
A college fest records attendance across multiple gates into a `HashSet<Student>`. A junior developer overrode `equals()` on `regNo` but **omitted `hashCode()`**.

### 1. Three Deliberately Designed Tests
1. **Test 1 (Appears to work correctly):** Scanning the exact same Java object reference (`arunCard`) twice.
   * *Prediction:* Set size = **1**.
   * *Observed:* Set size = **1**.
   * *Reason:* In `HashMap.putVal()`, reference equality (`p.key == key`) evaluates to `true` immediately before invoking `equals()`. Both scans share the same memory address.
2. **Test 2 (Exposes duplicate entries):** Creating two distinct `Student` instances with the same `regNo` ("192311001", "Arun") and one distinct student ("192311002", "Divya").
   * *Prediction:* Set size = **3** (expected 2).
   * *Observed:* Set size = **3**.
   * *Reason:* Because `hashCode()` is not overridden, default `Object.hashCode()` computes memory-derived hashes. The two instances land in different hash buckets; `equals()` is never evaluated!
3. **Test 3A (Mutating key field after insertion):** Inserting student with `regNo = "192311001"`, then mutating its field to `"192311999"`.
   * *Observed:* `set.contains(student)` returns `false`, and `set.remove(student)` fails.
   * *Reason:* The element is trapped in the bucket determined by its old hash. Lookups search the new bucket and find nothing (memory leak / zombie object).
4. **Test 3B (`TreeSet` inconsistent with `equals()`):** `TreeSet` with `compareTo()` on name only, while `equals()` is on `regNo`. Two students with different `regNo` but identical names added.
   * *Observed:* Set size = **1** (valid student silently discarded!).
   * *Reason:* `TreeSet` relies strictly on `compareTo() == 0` and completely ignores `equals()`.

### 2. Inquiry Answers
* **Why duplicates appear:** `HashSet` uses hash codes to route keys to buckets. If two equal objects have different hash codes, they are stored in separate buckets and never compared with `equals()`.
* **How `HashSet` locates elements:** 1) Computes `h = hash(key)`. 2) Computes bucket index `(capacity - 1) & h`. 3) Fast-rejects if bucket empty. 4) For bucket nodes, checks cached `p.hash == h` and `(p.key == key || key.equals(p.key))`.
* **How to correct `Student`:** Declare fields `private final` (immutability) and override `hashCode()` using the exact fields evaluated in `equals()`:
  ```java
  @Override public int hashCode() { return Objects.hashCode(regNo); }
  ```

### 3. "Complete the Code" Program Results
* **Before TODO 1:** `Unique students: 3` (Duplicates allowed due to mismatched identity hashes).
* **After TODO 1:** `Unique students: 2` (Correctly de-duplicated).

### 4. Alternative Concept & Complexity Analysis
* `HashSet<Student>`: $\mathcal{O}(1)$ average time per scan, $\approx 32\text{ bytes/node}$.
* `TreeSet<Student>` (with `Comparator` on `regNo`): $\mathcal{O}(\log N)$ guaranteed time, $\approx 40\text{ bytes/node}$. Keeps entries sorted but 3x slower.
* `HashMap<String, Student>` (Key: `regNo`): $\mathcal{O}(1)$ average time. Bypasses `Student.hashCode()` by utilizing Java's built-in immutable `String.hashCode()`.

---

# Task 04 – Choosing a Map for a Pharmacy Inventory
**Bloom's Level: K4 – Analyse | Marks: 6**

### Problem Context – Hospital Pharmacy Stock Lookup
Pharmacists look up stock by medicine code hundreds of times a day. Every evening, the system prints:
1. **Report 1:** All medicines in alphabetical **code order**.
2. **Report 2:** All medicines in **insertion order**.

### 1. Theoretical Expectations
* `HashMap`: $\mathcal{O}(1)$ put/get, undefined iteration order.
* `TreeMap`: $\mathcal{O}(\log N)$ put/get, natural sorted key order. Naturally produces Report 1.
* `LinkedHashMap`: $\mathcal{O}(1)$ put/get, insertion order. Naturally produces Report 2.
* Extra work for wrong map: `HashMap` and `LinkedHashMap` require an explicit $\mathcal{O}(N \log N)$ sort for Report 1. `HashMap` and `TreeMap` cannot produce Report 2 without duplicating data into an auxiliary list.

### 2. Measured Experimental Evidence Across Scales

| Scale ($N$) | Map Type | Put Time (ms) | 100,000 Lookups (ms) | Report 1 (Code Order) | Report 2 (Insertion Order) |
|---|---|---|---|---|---|
| **$N = 1,000$** | HashMap | 1.20 ms | **18.55 ms** | 11.03 ms (requires sort) | 0.22 ms (Order Invalid!) |
| | TreeMap | 5.37 ms | 42.17 ms | **0.98 ms (Natural)** | 0.25 ms (Order Invalid!) |
| | LinkedHashMap | 2.51 ms | **15.77 ms** | 5.21 ms (requires sort) | **0.15 ms (Natural)** |
| **$N = 10,000$** | HashMap | 1.90 ms | **24.24 ms** | 13.52 ms (requires sort) | 1.68 ms (Order Invalid!) |
| | TreeMap | 9.46 ms | 51.92 ms | **3.03 ms (Natural)** | 2.22 ms (Order Invalid!) |
| | LinkedHashMap | 1.99 ms | **17.41 ms** | 11.91 ms (requires sort) | **0.52 ms (Natural)** |
| **$N = 100,000$** | HashMap | 33.63 ms | **25.72 ms** | 93.72 ms (requires sort) | 4.44 ms (Order Invalid!) |
| | TreeMap | 143.93 ms | 132.82 ms | **24.01 ms (Natural)** | 9.77 ms (Order Invalid!) |
| | LinkedHashMap | **24.73 ms** | **23.73 ms** | 129.36 ms (requires sort) | **0.99 ms (Natural)** |

### 3. Scalability Inquiry & Deployment Decision
* **Acceptable Lookup Response Time:** $< 1.0\text{ ms}$ per barcode scan. `LinkedHashMap` and `HashMap` executed 100,000 lookups in $23.7\text{ ms}$, achieving **$0.237\,\mu\text{s}$ (237 nanoseconds) per lookup**—over **4,000x faster than required**.
* **Deployment Recommendation:** **`LinkedHashMap<String, Medicine>`**.
  * Matches `HashMap` sub-microsecond daytime lookup speed.
  * Preserves insertion order naturally throughout the day, generating Report 2 in **$0.99\text{ ms}$**.
  * Report 1 is generated only once per day in the evening; sorting 100k items in $129\text{ ms}$ is imperceptible to users.
* **Why `Hashtable` is Obsolete:** Every method is guarded by `synchronized (this)`, causing severe lock contention. Prohibits null keys/values. Modern applications use `HashMap` or `ConcurrentHashMap`.

### 4. Alternative: Sorted Array with `Arrays.binarySearch()`
* $\mathcal{O}(\log N)$ lookups, $\mathcal{O}(N)$ insertion shift penalty, smallest memory footprint. Unviable for active daytime medicine additions and cannot produce Report 2 without duplicate storage.

---

# Task 05 – Derive a Generic Ranking Component from a Principle
**Bloom's Level: K4 – Analyse | Marks: 7**

### Problem Context – Inter-College Sports Leaderboard
Rank teams by points (higher first), tie-broken by goal difference (higher first), then by team name (alphabetical). Must be reusable for players, colleges, and quiz teams.
* **Core Principle:** *"A ranking is correct only if the comparison used is consistent: if A ranks above B and B ranks above C, then A must rank above C, and equal items must compare as equal."*

### 1. Design Reasoning
* **`Comparable<Team>` vs Separate `Comparator`:** Separate `Comparator` objects are vastly superior. They adhere to the Single Responsibility Principle, allow multiple interchangeable ranking criteria (e.g. fair-play vs points), and allow `Leaderboard<T>` to remain 100% decoupled from domain entities.
* **Multi-Field Comparator Chaining:**
  ```java
  Comparator<Team> byRank = Comparator
      .comparingInt((Team t) -> t.points).reversed()
      .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
      .thenComparing((Team t) -> t.name);
  ```
* **PECS Generics:** `Leaderboard(Comparator<? super T> order)` allows comparators written for supertypes to rank subtypes. `addAll(List<? extends T> items)` accepts collections of subtypes.

### 2. "Complete the Code" Program Results
* **`LeaderboardDemo.java` Output:**
  ```
  [Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]
  ```
* **Why Bisons is Ranked Above Panthers:**
  Both teams tie with 9 points and 4 goal difference. The third tie-breaker evaluates `"Bisons".compareTo("Panthers")`. Because `'B'` precedes `'P'` alphabetically, Bisons ranks ahead.

### 3. Edge Case & Faulty Comparator Testing
* **Integer Subtraction Overflow Bug:** Naive subtraction `(t1, t2) -> t2.points - t1.points` with extreme values ($100$ vs $-2,147,483,648$) produced $+2147483548$ due to integer wrap-around, inverting rank order! Resolved using `Integer.compare()`.
* **Reusability Test:** Demonstrated identical `Leaderboard<QuizTeam>` ranking college quiz teams by score, penalties, and name without changing a single line of `Leaderboard<T>`.

### 4. Alternative Concept & Complexity Analysis
* Comparator chain executes in a single pass of $\mathcal{O}(N \log N)$ comparisons.
* Sorting 3 separate times with stable sorts requires $3 \times \mathcal{O}(N \log N)$ time, is 3x slower, and is prone to instability.

---

# Task 06 – Priority, Order and Fairness in a Queue
**Bloom's Level: K5 – Evaluate | Marks: 8**

### Problem Context – Hospital Emergency Triage
Patients arrive with severity 1 (critical) to 5 (minor). Highest severity (lowest integer) treated first; equal severity served First-Come, First-Served.

### 1. Queue Architecture
* **Why Plain Queue Fails:** Standard FIFO treats a sprained ankle arriving at 10:00 AM before a cardiac arrest arriving at 10:05 AM.
* **Why Direct Print Fails:** Printing `PriorityQueue` displays the backing binary heap array (where `parent <= children`), NOT the sorted treatment order.
* **Preserving Arrival Order:** Monotonically increasing atomic sequence counter (`counter++`) embedded in `Patient.arrival` acts as a deterministic secondary tie-breaker:
  ```java
  Comparator.comparingInt((Patient p) -> p.severity)
            .thenComparingLong(p -> p.arrival);
  ```

### 2. Hand-Verified Correctness Check (8 Patients)
* Input: Sita(S3,t0), Aman(S1,t1), Rajesh(S2,t2), Fatima(S1,t3), David(S4,t4), Ananya(S2,t5), Vikram(S5,t6), Sneha(S3,t7).
* Hand-derived: `Aman(S1) -> Fatima(S1) -> Rajesh(S2) -> Ananya(S2) -> Sita(S3) -> Sneha(S3) -> David(S4) -> Vikram(S5)`.
* Polled Program Output: **100% Match (PASSED)**.

### 3. Growth Benchmark Across Scales

| Queue Size ($N$) | Total Admit Time (ms) | Total Poll Time (ms) | Avg Time Per Op (ns) |
|---|---|---|---|
| **$N = 1,000$** | 10.14 ms | 2.71 ms | 6,421.6 ns |
| **$N = 10,000$** | 15.70 ms | 11.17 ms | 1,343.6 ns |
| **$N = 100,000$** | 54.43 ms | 67.48 ms | **609.5 ns** |

### 4. Starvation Inquiry & Fair Aging Rule
* **Starvation Cause:** Continuous arrivals of S1–S3 patients prevent S5 patients from ever reaching the root.
* **Aging Rule:** $\text{Effective Severity} = \max\left(1, \text{Original Severity} - \left\lfloor \frac{\text{Wait Time}}{\tau} \right\rfloor\right)$. Every $\tau = 15$ minutes, severity increases by 1 tier, guaranteeing critical care within 60 minutes.

### 5. Deployment Decision (5,000 Patients/Day)
* **Verdict: APPROVED.** 5,000 admissions/day consumes $< 5\text{ ms}$ of total CPU time per day and $\approx 240\text{ KB}$ of memory.

### 6. "Complete the Code" Program Results
* Output: `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)`. Exactly matches specification.

---

# Task 07 – Day-4 Design Challenge: Diagnose, Redesign and Validate
**Bloom's Level: K5 – Evaluate | Marks: 13**

### Selection: Redesign of Hospital Emergency Triage System (Q6)

### Part A: Diagnose the Bottleneck (2 Marks)
1. **Algorithmic Overhead:** `PriorityQueue` executes $\mathcal{O}(\log_2 N)$ sift-down tree swaps on every patient dispatch (62.99 ms for 100k items).
2. **Unnecessary Work:** Because severity levels are strictly bounded discrete integers ($1 \le S \le 5$), building and maintaining a comparison tree across 100,000 nodes wastes clock cycles.
3. **Starvation & Dynamic Invalidation:** The binary min-heap cannot dynamically update a patient's priority (aging) without silently corrupting heap order or incurring an $\mathcal{O}(N \log N)$ rebuild.

### Part B: Define the Design Objective (2 Marks)
1. Reduce admission and dispatch time complexity from $\mathcal{O}(\log N)$ to **strictly deterministic $\mathcal{O}(1)$ constant time**.
2. Eliminate patient starvation by bounding maximum wait time to $4 \times \tau$ minutes via dynamic priority aging.
3. Achieve at least a **2x overall throughput speedup** and **5x faster dispatch latency**.

### Part C: Design the Revised Solution (4 Marks)
* **Architecture:** Multi-Level Bucket Queue (Array of 5 `ArrayDeque<Patient>` instances, one per severity tier) with time-based priority escalation.
* **Admit:** `buckets[patient.currentSeverity].addLast(patient)` ($\mathcal{O}(1)$).
* **Poll Next:** Scan indices 1 to 5; return `buckets[tier].pollFirst()` from first non-empty tier ($\mathcal{O}(1)$).
* **Aging Sweep:** Periodically promote patients whose waiting time exceeds threshold $\tau$ to `buckets[tier - 1]`.

### Part D: Predict Before Implementing (1 Mark)
* Dispatch speedup will exceed 5x due to eliminating 17 levels of heap sift-downs. Overall speedup will be 2.0x–3.0x. Starvation will be completely eliminated.

### Part E: Implement and Validate the Redesign (2 Marks)

| Dataset Scale ($N$) | Architecture | Admit Time (ms) | Poll Time (ms) | Total Time (ms) | Speedup Factor |
|---|---|---|---|---|---|
| **$N = 1,000$** | Original `PriorityQueue` | 2.037 ms | 2.383 ms | 4.420 ms | 1.0x (Baseline) |
| | **Redesigned BucketTriage** | **0.749 ms** | **0.716 ms** | **1.465 ms** | **3.02x faster** |
| **$N = 10,000$** | Original `PriorityQueue` | 2.482 ms | 7.311 ms | 9.793 ms | 1.0x (Baseline) |
| | **Redesigned BucketTriage** | **2.296 ms** | **1.260 ms** | **3.555 ms** | **2.75x faster** |
| **$N = 100,000$** | Original `PriorityQueue` | 7.651 ms | 62.994 ms | 70.645 ms | 1.0x (Baseline) |
| | **Redesigned BucketTriage** | 24.247 ms | **3.681 ms** | **27.928 ms** | **2.53x faster** |

* **Dispatch Latency at $N = 100,000$:** Dropped from **$629.9\text{ ns}$** in `PriorityQueue` to **$36.8\text{ ns}$** in `BucketTriage` (**17.1x faster**).
* **Starvation Test:** In `PriorityQueue`, $S5$ patient Rahul starved indefinitely. In `BucketTriage`, Rahul was promoted $S5 \rightarrow S4 \rightarrow S3 \rightarrow S2 \rightarrow S1$ and received timely emergency care.

### Part F: Defend the Final Design (2 Marks)
* Prediction fully supported: 17.1x faster dispatch, 2.53x overall speedup, zero starvation.
* Trade-offs: Minor initial memory for 5 `ArrayDeque` headers ($< 1\text{ KB}$), negligible compared to clinical lives saved and deterministic latency guarantees.
* Situations where original is preferred: Unbounded or continuous floating-point priority metrics (e.g., shortest-path Dijkstra algorithms).
