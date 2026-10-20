# Quick Reference: CSV Import Methods

## 🚀 All 4 Methods at a Glance

### 1️⃣ Traditional (Single-threaded)
```bash
curl -X POST http://localhost:8080/api/books/import-csv-traditional \
  -F "file=@books.csv"
```
- ⚡ ~20K books/sec
- 💾 Low memory
- ⏱️ Synchronous
- 👍 Best for: Small files

---

### 2️⃣ Virtual Threads
```bash
curl -X POST http://localhost:8080/api/books/import-csv-virtual-threads \
  -F "file=@books.csv"
```
- ⚡ ~80K books/sec
- 💾 Medium memory
- ⏱️ Synchronous
- 👍 Best for: Large files (100K+)

---

### 3️⃣ Parallel Streams **NEW!**
```bash
curl -X POST http://localhost:8080/api/books/import-csv-parallel-stream \
  -F "file=@books.csv"
```
- ⚡ ~100K books/sec
- 💾 High memory (loads entire file)
- ⏱️ Synchronous
- 👍 Best for: Medium files (50K-500K)
- ✨ Uses ForkJoinPool
- ✨ Automatic CPU utilization

---

### 4️⃣ Fire-and-Forget
```bash
curl -X POST http://localhost:8080/api/books/import-csv-parallel-fire-and-forget \
  -F "file=@books.csv"
```
- ⚡ ~100K books/sec
- 💾 Medium memory
- ⏱️ **Asynchronous** (202 response)
- 👍 Best for: Any size, no client wait

---

## 📊 Quick Comparison

| Method | Speed | Memory | Wait | Best Use Case |
|--------|-------|--------|------|---------------|
| Traditional | ⭐⭐ | ⭐ | Yes | <10K records |
| Virtual Threads | ⭐⭐⭐⭐ | ⭐⭐ | Yes | 100K-1M+ |
| **Parallel Streams** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Yes | 50K-500K |
| Fire-and-Forget | ⭐⭐⭐⭐⭐ | ⭐⭐ | **No** | Any size |

---

## 🎯 Decision Tree

```
How many records?
├─ < 10K → Traditional
├─ 10K - 100K → Parallel Streams ✨
├─ 100K - 1M → Virtual Threads
└─ > 1M or timeout issues → Fire-and-Forget

Can client wait?
├─ Yes → Traditional / Virtual Threads / Parallel Streams
└─ No → Fire-and-Forget

Memory constrained?
├─ Yes → Traditional
└─ No → Virtual Threads / Parallel Streams

Want automatic CPU usage?
├─ Yes → Parallel Streams ✨
└─ No → Virtual Threads
```

---

## 🧪 Test All Methods
```bash
./test_parallel_stream.sh
```

---

## 📝 CSV Format
```csv
title,author,price
The Great Adventure,John Smith,29.99
"Book With, Comma",Jane Doe,19.99
```

---

## 📚 Full Documentation
- **Implementation Details:** [PARALLEL_STREAM_UPLOAD_IMPLEMENTATION.md](PARALLEL_STREAM_UPLOAD_IMPLEMENTATION.md)
- **Complete Comparison:** [CSV_IMPORT_METHODS_COMPARISON.md](CSV_IMPORT_METHODS_COMPARISON.md)
- **API Docs:** http://localhost:8080/swagger-ui/index.html

---

## 🔍 Monitor Performance
```bash
# View import logs
docker-compose logs app | grep "CSV IMPORT"

# Check throughput
docker-compose logs app | grep "Throughput"

# Verify count
curl -s http://localhost:8080/api/books | jq 'length'
```

---

## ⚡ New Parallel Streams Highlights

**What's Different:**
- Uses Java Streams API (`parallelStream()`)
- Automatic work distribution across CPU cores
- Functional programming style
- Simple, clean code

**Example Code:**
```java
List<Book> books = lines.parallelStream()
    .filter(line -> !line.isEmpty())
    .map(this::parseLine)
    .filter(Objects::nonNull)
    .toList();
```

**When to Use:**
- ✅ File fits in memory
- ✅ Want automatic CPU utilization
- ✅ Prefer functional style
- ✅ Medium-sized files (50K-500K)

**When NOT to Use:**
- ❌ Very large files (>500K, may cause OOM)
- ❌ Memory constrained environments
- ❌ Need streaming (use Traditional)

