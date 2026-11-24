# Task 06: Priority, Order and Fairness in a Queue

## 1. Problem Context
In a hospital emergency department (ED), arriving patients are assigned a triage severity level from **1 (Critical / Resuscitation)** to **5 (Non-urgent / Minor)**.
* **Clinical Rule 1:** The doctor on duty must always attend to the patient with the highest medical severity (lowest numerical severity level) first.
* **Clinical Rule 2 (Fairness within Tier):** Among patients sharing the same severity level, the patient who arrived first must be seen first (First-Come, First-Served within severity tier).

---

## 2. Queue Design Analysis

### 1. Why a Plain Queue (`LinkedList` or `ArrayDeque`) is Not Sufficient:
A standard `Queue` (whether backed by `LinkedList` or `ArrayDeque`) strictly adheres to global FIFO (First-In, First-Out) ordering. Under global FIFO, a patient with cardiac arrest (Severity 1) arriving at 10:15 AM would be placed behind a patient with a sprained ankle (Severity 5) who arrived at 10:10 AM. This is clinically unacceptable and life-threatening.

### 2. How `PriorityQueue` with a Composite Comparator Enforces the Rule:
Java’s `PriorityQueue` implements a binary min-heap. By configuring it with a two-level comparator:
```java
Comparator.comparingInt((Patient p) -> p.severity)
          .thenComparingLong(p -> p.arrival)
```
The root of the heap is guaranteed to always hold the patient with the minimum severity integer. When severities tie, the patient with the smaller arrival sequence number floats to the top.

### 3. How Arrival Order is Preserved Among Equal Severities:
Because Java's `PriorityQueue` is **not a stable collection** (it does not preserve natural insertion order for equal keys), stability must be engineered explicitly. An atomic monotonically increasing `counter` (`private long counter = 0;`) assigns a unique, increasing sequence number to each incoming patient at admission time (`counter++`). This sequence number acts as a strictly unique secondary key that guarantees deterministic FIFO tie-breaking.

### 4. How the Next Patient is Removed and Waiting List Displayed:
* **Removal:** `queue.poll()` extracts the highest-priority root patient in $\mathcal{O}(\log N)$ time and re-heapifies the tree in $\mathcal{O}(\log N)$.
* **Displaying Waiting List:** Printing `queue` or calling `queue.iterator()` **does NOT display patients in treatment order**. An array-backed binary heap only maintains the partial ordering invariant `parent <= left_child` and `parent <= right_child`. To display the waiting list in true treatment order without destroying the live queue, the system must create a shallow clone (`new PriorityQueue<>(queue)`) and poll from it, or copy entries to a list and sort them ($\mathcal{O}(N \log N)$).

---

## 3. Correctness Verification (8 Hand-Worked Patients)

### Test Data:
1. `Sita` (S3, Arrival: 0)
2. `Aman` (S1, Arrival: 1)
3. `Rajesh` (S2, Arrival: 2)
4. `Fatima` (S1, Arrival: 3)
5. `David` (S4, Arrival: 4)
6. `Ananya` (S2, Arrival: 5)
7. `Vikram` (S5, Arrival: 6)
8. `Sneha` (S3, Arrival: 7)

### Step-by-Step Hand Derivation:
* **Severity 1:** `Aman` (arr 1), `Fatima` (arr 3) $\rightarrow$ `Aman`, `Fatima`
* **Severity 2:** `Rajesh` (arr 2), `Ananya` (arr 5) $\rightarrow$ `Rajesh`, `Ananya`
* **Severity 3:** `Sita` (arr 0), `Sneha` (arr 7) $\rightarrow$ `Sita`, `Sneha`
* **Severity 4:** `David` (arr 4) $\rightarrow$ `David`
* **Severity 5:** `Vikram` (arr 6) $\rightarrow$ `Vikram`

**Expected Treatment Order:**
`Aman(S1) -> Fatima(S1) -> Rajesh(S2) -> Ananya(S2) -> Sita(S3) -> Sneha(S3) -> David(S4) -> Vikram(S5)`

### Experimental Output:
* **Direct Heap Layout:** `[Aman(S1,t1), Fatima(S1,t3), Rajesh(S2,t2), Sita(S3,t0), David(S4,t4), Ananya(S2,t5), Vikram(S5,t6), Sneha(S3,t7)]`
  *(Shows Sita(S3) placed before Ananya(S2) in the raw array, proving why raw iteration fails).*
* **Polled Order:** `[Aman(S1,t1), Fatima(S1,t3), Rajesh(S2,t2), Ananya(S2,t5), Sita(S3,t0), Sneha(S3,t7), David(S4,t4), Vikram(S5,t6)]`
* **Verification Match:** **100% PASSED**.

---

## 4. Growth Investigation & Benchmark

### Theoretical Prediction:
* **Admitting a patient (`add`):** $\mathcal{O}(\log N)$ worst-case as the new node is sifted up along the tree height $h = \lfloor \log_2 N \rfloor$.
* **Serving a patient (`poll`):** $\mathcal{O}(\log N)$ worst-case as the leaf node placed at the root is sifted down.

### Experimental Measurements:

| Queue Size ($N$) | Total Admit Time (ms) | Total Poll Time (ms) | Avg Time Per Operation (ns) |
|---|---|---|---|
| **$N = 1,000$** | 10.14 ms | 2.71 ms | 6,421.6 ns |
| **$N = 10,000$** | 15.70 ms | 11.17 ms | 1,343.6 ns |
| **$N = 100,000$** | 54.43 ms | 67.48 ms | **609.5 ns** |

*Analysis:* Even at a massive scale of 100,000 simultaneous active patients, both insertion and removal take **$< 0.7\,\mu\text{s}$ per operation**, confirming the logarithmic scalability of the binary heap.

---

## 5. Inquiry: Starvation & Fair Aging Rule Design

### 1. Why Severity-5 Patients Suffer Indefinite Starvation:
Under strict static priority scheduling, if high-severity cases (S1–S3) arrive at a rate equal to or faster than the doctor's treatment rate, the priority queue root will *always* be occupied by an S1, S2, or S3 patient. The Severity-5 patient will remain at the bottom of the heap indefinitely. In hospital emergency rooms, this leads to clinical deterioration and medical emergencies in the waiting hall.

### 2. Fair Aging Rule Design:
To eliminate starvation while preserving clinical safety, we introduce **Priority Aging**:
$$\text{Effective Severity} = \max\left(1, \text{Original Severity} - \left\lfloor \frac{\text{Wait Time}}{\tau} \right\rfloor\right)$$
* Where $\tau$ is the aging interval (e.g., 15 minutes).
* **Progression for Vikram (S5):**
  * Minute 0: S5 (Minor)
  * Minute 15: S4 (Elevated)
  * Minute 30: S3 (Urgent)
  * Minute 45: S2 (Emergent)
  * Minute 60: S1 (Critical - Guaranteed immediate treatment)
* This bounds the maximum waiting time of any patient to $4 \times \tau$ minutes.

---

## 6. Deployment Decision

### Question: Can this design serve a city hospital receiving 5,000 patients a day?
### Verdict: **YES, with flying colors.**
* **Throughput Justification:** 5,000 patients per day averages only $\approx 3.5$ admissions per minute ($\approx 0.058\text{ ops/sec}$). Peak surges might reach 50 patients per minute.
* **Latency:** Our benchmark demonstrates that `PriorityQueue` easily handles **100,000 admissions in 54.4 ms** ($0.54\,\mu\text{s}$ per admission). Processing 5,000 patients consumes a cumulative total of **$< 5\text{ milliseconds of CPU time per entire day}$**.
* **Memory Footprint:** 5,000 `Patient` objects consume approximately $\approx 240\text{ KB}$ of RAM, which is completely trivial on modern server hardware.

---

## 7. "Complete the Code" Program Results

### Execution of `Triage.java`:
```
Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)
```
*Verification:* Matches the problem specification output exactly.

---

## 8. Alternative Concept and Complexity Analysis

### Comparison of Three Architectural Queue Designs:

| Architecture | Admit Patient (`add`) | Serve Next Patient (`poll`) | Peak Space Complexity | Evaluation & Practical Applicability |
|---|---|---|---|---|
| **1. Standard `PriorityQueue`** (Binary Min-Heap) | $\mathcal{O}(\log N)$ | $\mathcal{O}(\log N)$ | $\mathcal{O}(N)$ (compact array) | Excellent general-purpose priority queue. Simple code, low memory. Suffers from starvation unless aged. |
| **2. Sorted `LinkedList`** (Insertion Sort) | $\mathcal{O}(N)$ (must traverse to find insertion spot) | $\mathcal{O}(1)$ (remove head) | $\mathcal{O}(N)$ ($24$ bytes/node) | **Unacceptable for high volume.** Admitting requires walking up to $N$ linked nodes. |
| **3. Array of 5 FIFO Queues** (Multi-Level Bucket Queue) | $\mathcal{O}(1)$ (`queues[sev].add()`) | $\mathcal{O}(1)$ (check indices 1 to 5) | $\mathcal{O}(N)$ (5 array deques) | **Optimal for bounded severities!** Because severity is strictly bounded between 1 and 5, an array of 5 `ArrayDeque` queues eliminates heap overhead entirely and achieves pure $\mathcal{O}(1)$ operations. |
