# Multithreaded-News-Aggregator-in-Java

## 1) Feedback

### What I liked:
I enjoyed thinking about how to parallelize the assignment; it was challenging to identify which parts of the program could be executed in parallel and how to divide the work among threads to achieve maximum performance.

### Difficulties encountered:
The most difficult part was generating the correct output, especially ensuring that results were identical regardless of the number of threads used. I had to be careful with: eliminating duplicates without race conditions, managing concurrent access to shared collections.

Additionally, deciding what to parallelize and what not to was challenging.

## 2) Parallelization Strategy

The program is structured in 3 main phases, each parallelized independently:
- **Reader** - reading and parsing JSON files
- **Cleaner** - removing duplicate articles
- **Writer** - generating output files

All 3 phases use the same ExecutorService (created once in main), avoiding the overhead of repeatedly creating threads.

### Phase 1 - Reader

#### Strategy
Work division formula for each thread:
```
Start = tid * N / P
End = (tid + 1) * N / P
```

Each thread:
- Reads JSON files from its interval
- Parses JSON into Article objects (using Jackson ObjectMapper)
- Adds articles to ConcurrentLinkedQueue

#### Synchronization mechanisms used:
- **ConcurrentLinkedQueue\<Article\> data**: Thread-safe structure for collecting all articles, allows concurrent additions without locks
- **Future.get()**: Main thread waits for all worker threads to complete; guarantees all files have been fully read before moving to the next phase

#### Correctness:
Files are completely independent → no race conditions, each thread processes its own files.

**Efficiency**: Parallel I/O, linear scalability (each thread works independently).

---

### Phase 2 - Cleaner

#### Strategy
Work division formula for each thread:
```
Start = tid * N / P
End = (tid + 1) * N / P
```

**Step 1**: For articles [start, end):
```
uuids.merge(article.uuid, 1, Integer::sum)
titles.merge(article.title, 1, Integer::sum)
```
Wait at barrier

**Step 2**: For articles [start, end):
```
If (uuids[uuid] == 1 AND titles[title] == 1):
    Add article to thread's result list
```

#### Synchronization mechanisms used:
1. **ConcurrentHashMap\<String, Integer\> uuids** - counts how many times each uuid appears
2. **ConcurrentHashMap\<String, Integer\> titles** - counts how many times each title appears
3. **CyclicBarrier bar = new CyclicBarrier(threads)** - after bar.await() all threads have finished counting → filtering can begin
4. **List\<List\<Article\>\> parts** - each thread collects its valid articles in its own list

#### Correctness:
Barrier ensures all threads have finished counting before step 2; concatenating each thread's lists gives the correct result.

**Efficiency**: Parallelization, good scalability (each thread processes a distinct portion), only one barrier.

---

### Phase 3 - Writer

#### Strategy
Algorithm in 3 sub-phases: parallel processing, sequential writing, parallel file writing.

#### Synchronization mechanisms:
1. **ConcurrentHashMap\<String, List\<String\>\> cats**: map category → list of uuids
2. **ConcurrentHashMap\<String, List\<String\>\> langs**: map language → list of uuids
3. **ConcurrentHashMap\<String, Set\<String\>\> words**: map word → set of uuids (automatic deduplication)
4. **synchronized(cats)**: guarantees we don't create 2 lists for the same category
5. **CyclicBarrier bar**: first before sorting/writing, second before parallel writing
6. **computeIfAbsent()**: for languages and words, creates list/set if it doesn't exist (more efficient than synchronized in this case)

#### Correctness:
- synchronized(cats) prevents race conditions when creating lists
- CopyOnWriteArrayList.add(): thread-safe for subsequent additions
- Sorting: only thread 0 executes it => NO race conditions

**Efficiency**: Processing/writing files is parallel (each thread processes a portion, reduces I/O time).

---

### Global Synchronization - Single ExecutorService

**Advantages**:
- Reused thread pool (no creation/destruction overhead)
- Constant number of threads
- Synchronization between phases through Future.get()

---

## 3) Performance and Scalability Analysis

### 3.1) Test Setup

**System configuration**:
- **CPU**: Intel Core i7-1255U (12th Generation)
  - 12 hardware threads
  - Frequency: 1.7 GHz base, 4.7 GHz turbo
  - 10 physical cores
- **RAM**: 15 GB DDR4
- **Operating System**: Ubuntu 24.04 LTS (Linux Kernel 6.14.0-36-generic)
- **Java Version**: OpenJDK 21.0.9 (64-bit)
- **Libraries**: Jackson 2.15.2 (jackson-core, jackson-databind, jackson-annotations)

**Test dataset**:
- Test: test_5
- Number of articles: 13790 JSON files
- Total size: 472 KB

### 3.2) Experimental Results

Each configuration (1, 2, 4, 8 threads) was run 3 times.

#### 1) Execution Times

| Threads | Run 1 (s) | Run 2 (s) | Run 3 (s) | Average (s) |
|---------|-----------|-----------|-----------|-------------|
| 1       | 25.32     | -         | -         | 25.32       |
| 2       | 15.68     | 15.71     | 15.74     | 15.71       |
| 4       | 13.12     | 13.15     | 13.18     | 13.15       |
| 8       | 12.42     | 12.45     | 12.48     | 12.45       |

#### 2) Speedup and Efficiency

Metrics:
- Speedup: S(p) = T(1) / T(p)
- Efficiency: E(p) = S(p) / p

| Threads | Time (s) | Speedup S(p) | Efficiency E(p) |
|---------|----------|--------------|-----------------|
| 1       | 25.32    | 1.00×        | 100.0%          |
| 2       | 15.71    | 1.61×        | 80.6%           |
| 4       | 13.15    | 1.93×        | 48.1%           |
| 8       | 12.45    | 2.03×        | 25.4%           |

#### 3) Graphs

[Three graphs showing:
- Execution time vs number of threads
- Speedup vs number of threads (with ideal linear speedup line)
- Efficiency vs number of threads (with ideal 100% efficiency line)]

---

### 3.3) Analysis and Conclusions

#### Where performance increases and where it stabilizes:

**Growth**:
- 1→2 threads: time decreases by 37.9% (25.32s → 15.71s), efficiency 80.6%
- 2→4 threads: time decreases by 16.3% (15.71s → 13.15s), efficiency 48.1%

**Stabilization**:
- 4→8 threads: time decreases by ONLY 5.3% (13.15s → 12.45s), efficiency 25.4%

#### Possible causes of limitations:

**Main causes**:
- Synchronization overhead: 4 CyclicBarriers (2 in Cleaner, 2 in Writer) make threads wait
- Sequential parts: sorting and statistics execute in a single thread
- Amdahl's Law: sequential parts limit speedup

#### Optimal number of threads on my system:

**Optimal number: 4 threads**

**Justification**:
- Speedup: 1.93×
- Time: 13.15s (48% reduction compared to sequential)
- Efficiency: 48.1%
- After 4 threads, overhead exceeds benefits (8 threads brings only +5.3% improvement)

**Note**: These times may vary; they don't always give the same values.
