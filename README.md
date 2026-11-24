# CSA09 – Programming in Java – Assignment 2 (CO2)
**Course:** CSA09 – Programming in Java – Assignments CO1–CO5  
**Topic Coverage:** Strings & StringBuilder, Collection Framework (List, Queue, Deque, PriorityQueue, Set, Map), hashCode–equals Contract, Generics, Comparable & Comparator, and Collections utility class (Sessions 15–26).  
**Student Name:** Anushk Ramesh  
**GitHub Username:** [Anushk-2006](https://github.com/Anushk-2006)  
**Register Number:** `<Register-Number>`  
**Java Version:** OpenJDK 21.0.12.1 LTS (build 21.0.12.1+1-LTS, Microsoft HotSpot 64-Bit Server VM)  
**GitHub Repository Link:** [https://github.com/Anushk-2006/Assessment-2.git](https://github.com/Anushk-2006/Assessment-2.git)

---

## Repository Structure & Task Overview

```
Assessment-2/
├── Task01/                     # Q1: String, StringBuilder & StringBuffer Under Load
│   ├── TicketBenchmark.java    # Benchmark testing 3 contrasting workloads
│   ├── output.txt              # Captured experimental output
│   └── README.md               # Predictions, complexity derivations, and design decision
├── Task02/                     # Q2: When Is ArrayList an Appropriate Engineering Choice?
│   ├── Song.java               # Music domain entity
│   ├── PlaylistBenchmark.java  # Benchmark: ArrayList vs LinkedList vs ArrayDeque
│   ├── output.txt              # Measurements across N = 1k, 10k, 100k
│   └── README.md               # Usage pattern boundaries, Up Next queue analysis
├── Task03/                     # Q3: Engineer the hashCode–equals Contract
│   ├── Attendance.java         # "Complete the Code" (TODO 1)
│   ├── HashContractExperiment.java # 3 edge-case tests: identical ref, exposed duplicates, mutable key & TreeSet
│   ├── output.txt              # Completed TODO 1 output (Unique: 2)
│   ├── output_before_todo.txt  # Output before TODO 1 (Unique: 3)
│   └── README.md               # Inquiry answers, bucket hashing mechanics, alternative structures
├── Task04/                     # Q4: Choosing a Map for a Pharmacy Inventory
│   ├── Medicine.java           # Pharmacy inventory entity
│   ├── PharmacyInventory.java  # Benchmark: HashMap vs TreeMap vs LinkedHashMap
│   ├── output.txt              # 100k lookups & two report generation benchmarks
│   └── README.md               # Scalability inquiry, Hashtable obsolescence, sorted array comparison
├── Task05/                     # Q5: Derive a Generic Ranking Component from a Principle
│   ├── LeaderboardDemo.java    # "Complete the Code" (TODO 1 & 2)
│   ├── LeaderboardTest.java    # Multi-level ties test, subtraction overflow bug test, reusability test
│   ├── output.txt              # Demo output matching expected specification
│   ├── test_output.txt         # Edge case test results
│   └── README.md               # Transitivity proof, comparator chaining, PECS wildcard analysis
├── Task06/                     # Q6: Priority, Order and Fairness in a Queue
│   ├── Triage.java             # "Complete the Code" (TODO 1)
│   ├── EmergencyTriageExperiment.java # 8-patient hand verification, growth benchmark, starvation test
│   ├── triage_output.txt       # Demo output matching expected specification
│   ├── output.txt              # Raw experimental data & aging progression
│   └── README.md               # Heap layout analysis, arrival tie-breaking, hospital deployment decision
├── Task07_DesignChallenge/     # Q7: Day-4 Design Challenge – Diagnose, Redesign and Validate
│   ├── EmergencyTriageRedesign.java # Original PriorityQueue vs Redesigned Multi-Level Bucket Queue with Aging
│   ├── output.txt              # Comparative benchmark and fairness test results
│   └── README.md               # Complete 6-part engineering design defense (A to F)
├── Assignment_Report.md        # Consolidated comprehensive submission report (Q1 to Q7)
└── README.md                   # This project index
```

---

## Quick Compilation and Execution Guide

Ensure Java 21 (or Java 17+) is installed. Run the commands from the root directory of this repository:

### Task 01: String vs StringBuilder vs StringBuffer
```bash
javac Task01/TicketBenchmark.java
java Task01.TicketBenchmark
```
* **Result Note:** Evaluates string concatenation across 3 workloads. Demonstrates that for a single 5,000-field manifest, `StringBuilder` is **404x faster** than repeated `+` concatenation ($0.31\text{ ms}$ vs $127.47\text{ ms}$), completely avoiding 5,000 intermediate heap allocations.

---

### Task 02: Music Playlist Editor (ArrayList vs LinkedList vs ArrayDeque)
```bash
javac Task02/Song.java Task02/PlaylistBenchmark.java
java Task02.PlaylistBenchmark
```
* **Result Note:** Random access `get(k)` on $100,000$ elements is **2,467x faster** in `ArrayList` ($2.37\text{ ms}$) than `LinkedList` ($5,848\text{ ms}$). For the "Up Next" queue, `ArrayDeque` delivers the highest throughput (**17.7x faster** than `ArrayList`), eliminating $O(N)$ shift penalties with zero node allocation overhead.

---

### Task 03: Event Attendance De-duplication (hashCode–equals Contract)
```bash
# 1. Complete the Code Program (Attendance.java)
javac Task03/Attendance.java
java Task03.Attendance

# 2. Comprehensive Contract Experiments
javac Task03/HashContractExperiment.java
java Task03.HashContractExperiment
```
* **Result Note:** Running `Attendance.java` before TODO 1 outputs `Unique students: 3` (duplicates allowed because `Object.hashCode()` hashed distinct instances to different buckets). After implementing TODO 1 (`Objects.hashCode(regNo)`), the output correctly produces `Unique students: 2`. Also validates mutated key corruption and `TreeSet` inconsistency.

---

### Task 04: Hospital Pharmacy Inventory (HashMap vs TreeMap vs LinkedHashMap)
```bash
javac Task04/Medicine.java Task04/PharmacyInventory.java
java Task04.PharmacyInventory
```
* **Result Note:** 100,000 lookups take only **$23.7\text{ ms}$** in `LinkedHashMap` and `HashMap` ($0.237\,\mu\text{s}$ per lookup), beating `TreeMap` ($132.8\text{ ms}$) by **5.6x**. `LinkedHashMap` naturally produces the daytime insertion report in **$0.99\text{ ms}$**, making it the optimal deployment choice for a busy pharmacy.

---

### Task 05: Inter-College Sports Leaderboard (Generic Ranking Component)
```bash
# 1. Complete the Code Program (LeaderboardDemo.java)
javac Task05/LeaderboardDemo.java
java Task05.LeaderboardDemo

# 2. Principle Edge Case & Reusability Test
javac Task05/LeaderboardTest.java
java Task05.LeaderboardTest
```
* **Result Note:** Successfully executes `LeaderboardDemo`, printing `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]`. Bisons ranks above Panthers because both tie on points ($9$) and goal difference ($4$), so the alphabetical tie-breaker ranks "Bisons" ahead of "Panthers". Exposes an integer subtraction overflow bug and validates decoupled reusability on `QuizTeam`.

---

### Task 06: Emergency Department Triage (Priority, Order and Fairness)
```bash
# 1. Complete the Code Program (Triage.java)
javac Task06/Triage.java
java Task06.Triage

# 2. Hand Verification, Growth Benchmark & Starvation Test
javac Task06/EmergencyTriageExperiment.java
java Task06.EmergencyTriageExperiment
```
* **Result Note:** Output produces `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)`. Shows why direct printing of `PriorityQueue` displays internal binary heap array order rather than sorted treatment order. Verifies 100% match with hand calculations across 8 patients and demonstrates that 5,000 daily patients consume $< 5\text{ ms}$ of total CPU time.

---

### Task 07: Day-4 Design Challenge (Diagnose, Redesign and Validate)
```bash
javac Task07_DesignChallenge/EmergencyTriageRedesign.java
java Task07_DesignChallenge.EmergencyTriageRedesign
```
* **Result Note:** Replaces the $O(\log N)$ comparison heap with a **Multi-Level Bucket Queue (5 ArrayDeques)** paired with a **Dynamic Aging Rule**. Achieves:
  * **17.1x faster patient dispatch** ($3.68\text{ ms}$ vs $62.99\text{ ms}$ for $100,000$ patients).
  * **2.53x overall throughput speedup**.
  * Complete elimination of patient starvation (an $S5$ patient is systematically promoted to $S1$ after waiting $60$ minutes).

---

## Summary of Results

| Task | Core Data Structure Investigated | Primary Empirical Finding | Practical Architectural Recommendation |
|---|---|---|---|
| **Q1** | `String` vs `StringBuilder` vs `StringBuffer` | `StringBuilder` is **404x faster** than `+` on large text blocks; avoids 500k objects on 10k tickets. | Use `StringBuilder` for thread-confined ticket rendering; use thread-safe queues/asynchronous loggers for shared logs. |
| **Q2** | `ArrayList` vs `LinkedList` vs `ArrayDeque` | `ArrayList` is **2,467x faster** for random access; `ArrayDeque` is **17.7x faster** for double-ended queues. | Use `ArrayList` for read/playback playlists; use `ArrayDeque` for "Up Next" queue. |
| **Q3** | `HashSet` & `hashCode`–`equals` Contract | Missing `hashCode()` silently broke de-duplication (size 3 instead of 2). Mutating keys corrupts lookups. | Always override both `equals()` and `hashCode()` on immutable `final` business keys. |
| **Q4** | `HashMap` vs `TreeMap` vs `LinkedHashMap` | `LinkedHashMap` matches `HashMap` speed ($23.7\text{ ms}$ / 100k gets) and naturally preserves insertion order ($0.99\text{ ms}$). | Deploy `LinkedHashMap`; sort once per evening for code-order report. |
| **Q5** | Generics, `Comparable` & `Comparator` | Comparator chaining cleanly resolves multi-tier ties; naive subtraction suffers integer overflow. | Keep entities as plain models; supply decoupled `Comparator<? super T>` to `Leaderboard<T>`. |
| **Q6** | `PriorityQueue` & FIFO Tie-Breaking | Heap array layout does not equal sorted treatment order. Arrival sequence counter guarantees strict FIFO within tiers. | System easily handles 5,000 patients/day; requires priority aging to prevent indefinite starvation. |
| **Q7** | Multi-Level Bucket Queue with Aging | Redesign achieved **17.1x faster dispatch** and solved patient starvation with bounded wait time. | Deploy multi-level discrete bucket queues when priority levels are bounded integers. |
