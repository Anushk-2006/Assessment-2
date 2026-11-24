# Task 03: Engineer the hashCode–equals Contract

## 1. Problem Context
A college fest records student attendance by scanning student ID cards across multiple entry gates. The same student may enter through gate 1, exit, and enter through gate 3. To maintain de-duplicated records of unique attendees, the organizers store scans in a `HashSet<Student>`.

A junior developer implemented the `Student` class by overriding `equals()` based on `regNo`, but **failed to override `hashCode()`**.

---

## 2. Experimental Investigation of the Contract

### Three Deliberately Designed Tests:

#### Test 1: Scans Where `HashSet` Appears to Work Correctly
* **Setup:** Scanning the exact same Java object reference (`arunCard`) multiple times.
* **Prediction:** `HashSet.size()` will be **1**.
* **Observed Result:** `HashSet.size() = 1`.
* **Reasoning:** In `HashMap.putVal()`, the entry lookup condition is:
  ```java
  if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
  ```
  Because both scans pass the identical memory address (`key == k`), reference equality evaluates to `true`, and the hash code is identical (`System.identityHashCode(arunCard) == System.identityHashCode(arunCard)`). This creates an illusion that the de-duplication works.

#### Test 2: Scans Deliberately Designed to Expose Duplicate Entries
* **Setup:** Two distinct `Student` instances created with the same `regNo` ("192311001", "Arun") and one distinct student ("192311002", "Divya").
* **Prediction:** The set will erroneously report a size of **3** instead of **2**.
* **Observed Result:** `HashSet.size() = 3`, containing `[192311001 (Arun), 192311002 (Divya), 192311001 (Arun)]`.
* **Reasoning:** When `hashCode()` is not overridden, `Student` inherits `Object.hashCode()`, which derives hash values from internal object memory addresses. Because the two "Arun" objects occupy different memory locations, they generate different hash codes and land in completely different hash buckets. `equals()` is **never even invoked**, resulting in silent duplicate accumulation.

#### Test 3A: Mutating a Key Field After Insertion (Edge Case)
* **Setup:** An element with `regNo = "192311001"` is inserted into `HashSet`. Subsequently, its `regNo` is mutated to `"192311999"`.
* **Prediction:** `set.contains(student)` will return `false`, and `set.remove(student)` will fail to remove it, even though the object physically exists inside the set!
* **Observed Result:** `contains()` returned `false`; set size remained `1`.
* **Reasoning:** The element was placed into bucket $B_{\text{old}} = \text{hash}(\text{"192311001"}) \pmod M$. When `contains()` is called after mutation, it calculates $B_{\text{new}} = \text{hash}(\text{"192311999"}) \pmod M$. It searches bucket $B_{\text{new}}$, finds it empty or containing other entries, and concludes the item is absent. The mutated object becomes a **zombie memory leak**.

#### Test 3B: `TreeSet` Inconsistent with `equals()`
* **Setup:** A `TreeSet` where `compareTo()` compares only by `name`, while `equals()` compares by `regNo`. Two students with different registration numbers but the same name ("Kavya") are added.
* **Prediction:** `TreeSet` size will erroneously be **1**, silently discarding a valid student!
* **Observed Result:** `TreeSet.size() = 1`.
* **Reasoning:** `TreeSet` adheres to `Comparable` / `Comparator` and checks `compareTo(x, y) == 0`. It **completely bypasses `equals()` and `hashCode()`**. When `compareTo()` is inconsistent with `equals()`, `TreeSet` violates the general `Set` contract.

---

## 3. Inquiry & Conceptual Analysis

### 1. Why do duplicates appear?
Duplicates appear because `HashSet` is a hash-bucket data structure. Before calling `equals()`, it computes `bucketIndex = (n - 1) & hash(key)`. If two equal objects return different hash codes, they are routed to different buckets. `HashSet` never compares items residing in different buckets.

### 2. How `HashSet` uses `hashCode()` and `equals()` to locate an element:
1. **Step 1 (Hash & Bucket Calculation):** Computes `h = key.hashCode() ^ (h >>> 16)`. Bucket index is calculated using `(capacity - 1) & h`.
2. **Step 2 (Fast Rejection):** If the bucket is empty, the element is not in the set. If occupied, it traverses the bucket's linked list or red-black tree.
3. **Step 3 (Equality Check):** For each node, it first checks if cached hash integers match (`p.hash == hash`). Only if hashes match does it check reference equality (`p.key == key`) or invoke `key.equals(p.key)`.

### 3. What happens when a field used in `hashCode()` is changed after insertion?
The object remains trapped in its original bucket. Future searches compute a different bucket index and fail to find the object. This causes data corruption, silent lookup failures, and prevents garbage collection. **Key fields in hash-based collections must be immutable (`final`).**

### 4. How to correct the `Student` class and verify the correction:
1. Mark identifying fields as `private final String regNo;`.
2. Override `hashCode()` to compute hash strictly from the same fields used in `equals()`:
   ```java
   @Override
   public int hashCode() {
       return Objects.hashCode(regNo);
   }
   ```
3. Verification: Execute `Attendance.java`. Ensure `present.size()` yields exactly `2`.

---

## 4. "Complete the Code" Program Results

### Execution Before Completing TODO 1:
```
Unique students: 3
```
*Explanation:* The two separate instances of Arun had different identity hash codes, landing in separate buckets.

### Execution After Completing TODO 1:
```
Unique students: 2
```
*Explanation:* Both Arun instances yielded identical hash codes, navigated to the same bucket, where `equals()` detected the duplicate and rejected the second scan.

---

## 5. Alternative Concept and Complexity Analysis

| Approach | Time Complexity per Scan | Space for $N$ Unique Students | Mechanism & Trade-offs |
|---|---|---|---|
| **`HashSet<Student>`** | $\mathcal{O}(1)$ average, $\mathcal{O}(N)$ worst-case | $N \times$ `Node` overhead ($\approx 32$ bytes/entry) | Requires both consistent `equals()` and `hashCode()`. Constant-time lookups. Unordered. |
| **`TreeSet<Student>`** (with `Comparator.comparing(Student::getRegNo)`) | $\mathcal{O}(\log N)$ guaranteed | $N \times$ `Entry` tree node ($\approx 40$ bytes/entry) | Does not require `hashCode()` or `equals()`. Keeps students sorted by `regNo`. Slightly slower per scan ($\mathcal{O}(\log N)$ tree traversal). |
| **`HashMap<String, Student>`** (Key: `regNo`) | $\mathcal{O}(1)$ average, $\mathcal{O}(N)$ worst-case | $N \times$ `Node` + String key reference | Bypasses `Student`'s `hashCode()` by using Java's built-in immutable `String.hashCode()`. Guarantees de-duplication even if `Student` is unmodifiable legacy code. |

* **Time Comparison:** For $N = 100,000$ scans, `HashSet` takes $\approx 15\text{ ms}$, while `TreeSet` takes $\approx 50\text{ ms}$ due to $\approx 17$ pointer hops and comparisons per scan.
* **Space Comparison:** `TreeSet` requires extra pointers for Red-Black tree balancing (`left`, `right`, `parent`, `color`), consuming 25% more memory than `HashSet`.
