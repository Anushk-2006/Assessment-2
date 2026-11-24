# Task 05: Derive a Generic Ranking Component from a Principle

## 1. Problem Context
An inter-college sports meet ranks teams by:
1. **Points** (higher first)
2. **Goal Difference** (higher first, as primary tie-breaker)
3. **Team Name** (alphabetical A–Z, as secondary tie-breaker)

The ranking engine must be built around a core design principle:
> *"A ranking is correct only if the comparison used is consistent: if A ranks above B and B ranks above C, then A must rank above C, and equal items must compare as equal."*

This component must be completely reusable for ranking other entities (players, colleges, quiz teams).

---

## 2. Design Reasoning & Architecture

### 1. `Comparable<Team>` vs. Separate `Comparator<Team>`
* **Verdict:** **Separate `Comparator` objects are significantly better.**
* **Justification:**
  * **Open/Closed Principle & Single Responsibility:** The `Team` class is a plain data model representing sports attributes. It should not hardcode business ranking rules.
  * **Multiple Orderings:** Different contexts require different rankings (e.g., alphabetical directory listing, ranking by fair play/least yellow cards, ranking by points). Implementing `Comparable` forces a single rigid "natural" order.
  * **Decoupling & Reusability:** By decoupling the comparison logic, `Leaderboard<T>` can rank *any* class `T` as long as a `Comparator<? super T>` is supplied.

### 2. Expressing Multi-Field Comparison
Java 8+ fluent combinators cleanly express hierarchical tie-breaking without nested `if-else` blocks:
```java
Comparator<Team> byRank = Comparator
    .comparingInt((Team t) -> t.points).reversed()
    .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
    .thenComparing((Team t) -> t.name);
```
* `comparingInt(...).reversed()` sorts higher points first.
* `thenComparing(...)` is only evaluated when the preceding comparator yields `0` (a tie).
* Primitive specialization (`comparingInt`) eliminates auto-boxing overhead.

### 3. Generics, Bounded Types, and Wildcards
* In `Leaderboard<T>`:
  ```java
  public class Leaderboard<T> {
      private final List<T> items = new ArrayList<>();
      private final Comparator<? super T> order;
      public Leaderboard(Comparator<? super T> order) { this.order = order; }
  }
  ```
* **Why `Comparator<? super T>`? (PECS Principle):**
  The comparator *consumes* instances of `T` to compare them. Using `? super T` allows a general comparator defined on a superclass (e.g., `Comparator<Participant>` or `Comparator<Object>`) to rank a specific subtype `Leaderboard<StudentAthlete>`.
* **Where `List<? extends T>` is useful:**
  If `Leaderboard` provides a bulk ingestion method:
  ```java
  public void addAll(List<? extends T> newItems) {
      this.items.addAll(newItems);
  }
  ```
  `newItems` is a *producer* of `T`. This permits adding a `List<FootballTeam>` into a `Leaderboard<Team>`.

---

## 3. "Complete the Code" Program Execution

### Program Code:
* Completed `TODO 1` in `Leaderboard.ranking()`:
  ```java
  List<T> sortedCopy = new ArrayList<>(items);
  sortedCopy.sort(order);
  return sortedCopy;
  ```
  *Crucial:* Returns a defensive copy so that ranking does not mutate the internal insertion order of `items`.
* Completed `TODO 2` in `LeaderboardDemo.main()`:
  ```java
  Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
          .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
          .thenComparing((Team t) -> t.name);
  ```

### Output:
```
[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]
```

### Explanation of Why Bisons is Ranked Above Panthers:
1. **Level 1 (Points):** Eagles (9), Bisons (9), Panthers (9) tie with 9 points. Cobras has 6 points and ranks last.
2. **Level 2 (Goal Difference):** Eagles has $+6$, ranking #1. Both Bisons and Panthers have $+4$, creating a tie.
3. **Level 3 (Alphabetical Tie-breaker):** The tie-breaker compares team names: `"Bisons".compareTo("Panthers")`. Because `'B'` precedes `'P'`, `"Bisons"` comes first alphabetically. Hence, Bisons is definitively ranked above Panthers.

---

## 4. Test Suite & Edge Case Investigation

### Test 1: Multi-level Ties Test
* Input: `Tigers(10,5)`, `Lions(10,5)`, `Bears(10,7)`, `Wolves(8,12)`.
* Output: `[Bears(10,7), Lions(10,5), Tigers(10,5), Wolves(8,12)]`.
* Verified that points take absolute priority over goal difference, goal difference breaks points ties, and name breaks goal difference ties.

### Test 2: Breaking Faulty Comparator (Integer Subtraction Overflow)
* Naive implementation: `(t1, t2) -> t2.points - t1.points`.
* When comparing `Alpha` (points = $100$) with `Omega` (points = $-2,147,483,648$, e.g., severe penalty points):
  $$\text{difference} = -2147483648 - 100 = +2147483548\quad (\text{Integer Underflow/Overflow!})$$
* Because the result wrapped around to a positive integer, the faulty comparator erroneously declared `Omega` greater than `Alpha`!
* **Resolution:** Always use `Integer.compare(x, y)` or `Comparator.comparingInt()`, which safely uses relational operators (`<`, `>`).

### Test 3: Decoupled Reusability on `QuizTeam`
* The exact same `Leaderboard<T>` was instantiated for `QuizTeam` without changing a single line of `Leaderboard`:
  ```
  Quiz Leaderboard Ranking:
  [BITS Pilani(Score:240, Pen:1), IIT Bombay(Score:240, Pen:2), NIT Trichy(Score:210, Pen:0)]
  ```

---

## 5. Correctness Inquiry (Transitivity Proof)

The comparator satisfies the mathematical strict weak ordering:
1. **Irreflexivity:** For any team $A$, `compare(A, A) == 0`.
2. **Antisymmetry:** If `compare(A, B) < 0`, then `compare(B, A) > 0`.
3. **Transitivity of Superiority:**
   * If `compare(A, B) < 0` and `compare(B, C) < 0`, then `compare(A, C) < 0`.
   * *Proof:* Suppose $A$ beats $B$ at field $k \in \{\text{points}, \text{goalDiff}, \text{name}\}$ and $B$ beats $C$ at field $m$.
     * If $k < m$, $A$ beats $B$ at an earlier field where $B$ and $C$ tie, so $A$ beats $C$ at field $k$.
     * If $k == m$, transitivity holds because `Integer.compare` and `String.compareTo` are mathematically transitive.
4. **Transitivity of Equivalence:** If $A$ ties $B$ across all three fields and $B$ ties $C$, then $A$ ties $C$.

---

## 6. Alternative Concept & Complexity Analysis

### Comparison of 3 Architectures:

| Approach | Time Complexity | Auxiliary Space | Extensibility & Reusability |
|---|---|---|---|
| **1. Modern Comparator Chain** (`Leaderboard<T>`) | $\mathcal{O}(N \log N)$ (single sort pass) | $\mathcal{O}(N)$ (for defensive output copy) | **Highest.** Works for any type $T$, zero modification to domain classes, interchangeable sorting rules. |
| **2. Intrinsic `Comparable<Team>`** | $\mathcal{O}(N \log N)$ (single sort pass) | $\mathcal{O}(N)$ | **Low.** Couples domain entity with ranking rules. Cannot support multiple ranking criteria. |
| **3. Three Sequential Stable Sorts** (Name $\rightarrow$ GD $\rightarrow$ Points) | $3 \times \mathcal{O}(N \log N)$ (three full sort passes) | $\mathcal{O}(N)$ | **Poor.** Requires 3 full sorting passes relying on stable sort (TimSort). 3x slower and fragile. |

* **Analysis:** Modern `Comparator` chaining executes a single sorting pass ($\mathcal{O}(N \log N)$) and is significantly easier to reuse for players, colleges, and tournaments.
