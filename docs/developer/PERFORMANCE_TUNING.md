# Performance Tuning Guide

This guide provides recommendations for optimizing the Eternity Puzzle Solver's performance for different use cases and hardware configurations.

## Table of Contents

- [Quick Start](#quick-start)
- [JVM Configuration](#jvm-configuration)
- [Thread Configuration](#thread-configuration)
- [Memory Management](#memory-management)
- [Algorithm Selection](#algorithm-selection)
- [Profiling and Monitoring](#profiling-and-monitoring)
- [Benchmarking](#benchmarking)
- [Hardware Recommendations](#hardware-recommendations)

## Quick Start

### Default Configuration (Recommended)

For most users, the default configuration provides excellent performance:

```bash
java -jar eternity-solver-1.0.0.jar example_4x4
```

### High-Performance Configuration

For large puzzles (6×6 and above) on multi-core systems:

```bash
java -Xmx4G -XX:+UseG1GC -XX:ParallelGCThreads=2 \
     -jar eternity-solver-1.0.0.jar -p -t 8 puzzle_6x12
```

### Memory-Constrained Configuration

For systems with limited RAM:

```bash
java -Xmx512M -XX:+UseSerialGC \
     -jar eternity-solver-1.0.0.jar -q example_4x4
```

## JVM Configuration

### Heap Size

#### Guidelines

| Puzzle Size | Recommended Heap | Minimum Heap |
|-------------|------------------|--------------|
| 3×3 to 4×4 | 256 MB | 128 MB |
| 5×5 to 6×6 | 1 GB | 512 MB |
| 7×7 to 10×10 | 2 GB | 1 GB |
| 11×11 to 16×16 | 4-8 GB | 2 GB |

#### Setting Heap Size

```bash
# Initial and maximum heap
java -Xms1G -Xmx4G -jar eternity-solver.jar puzzle

# Let JVM decide initial size
java -Xmx4G -jar eternity-solver.jar puzzle
```

#### Warning Signs

- **OutOfMemoryError**: Increase `-Xmx`
- **Excessive GC**: Increase heap size or optimize algorithm settings
- **Slow startup**: Decrease `-Xms` or remove it

### Garbage Collection

#### Recommended GC: G1 (Default on Java 11+)

```bash
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
     -XX:ParallelGCThreads=2 -jar eternity-solver.jar puzzle
```

**Best for**: Most workloads, especially parallel solving

#### Alternative: Parallel GC

```bash
java -XX:+UseParallelGC -XX:ParallelGCThreads=4 \
     -jar eternity-solver.jar puzzle
```

**Best for**: Throughput-oriented workloads, batch processing

#### Alternative: Serial GC

```bash
java -XX:+UseSerialGC -jar eternity-solver.jar puzzle
```

**Best for**: Single-core systems, memory-constrained environments

#### GC Tuning Parameters

```bash
# G1 GC Configuration
-XX:+UseG1GC                    # Enable G1
-XX:MaxGCPauseMillis=200        # Target pause time (ms)
-XX:ParallelGCThreads=2         # GC thread count
-XX:ConcGCThreads=1             # Concurrent marking threads
-XX:InitiatingHeapOccupancyPercent=45  # GC trigger threshold

# GC Logging (Java 11+)
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M
```

### JIT Compilation

#### C2 Compiler (Default)

```bash
java -XX:+TieredCompilation -XX:TieredStopAtLevel=4 \
     -jar eternity-solver.jar puzzle
```

**Best for**: Long-running solves (> 30 seconds)

#### Aggressive Optimization

```bash
java -XX:+AggressiveOpts -XX:CompileThreshold=1000 \
     -jar eternity-solver.jar puzzle
```

**Best for**: Maximum performance on puzzles taking minutes/hours

### Complete JVM Command Example

```bash
java -Xmx4G \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:ParallelGCThreads=2 \
     -XX:+TieredCompilation \
     -XX:+AggressiveOpts \
     -jar eternity-solver-1.0.0.jar -p -t 8 puzzle_6x12
```

## Thread Configuration

### Parallel Mode

#### Determining Optimal Thread Count

```bash
# Auto-detect (recommended): 75% of available cores
java -jar eternity-solver.jar -p puzzle

# Manual specification
java -jar eternity-solver.jar -p -t 8 puzzle
```

#### Guidelines

| Hardware | Recommended Threads | Notes |
|----------|-------------------|-------|
| 4 cores | 3 | Reserve 1 core for OS |
| 8 cores | 6-7 | 75-87% utilization |
| 16 cores | 12-14 | Diminishing returns above 12 |
| 32+ cores | 16-24 | Test for optimal value |

#### Thread Count Formula

```
Optimal_Threads = floor(Physical_Cores × 0.75)

For hyperthreading systems:
Optimal_Threads = floor(Physical_Cores × 0.75 to 1.0)
```

#### When to Use Parallel Mode

| Puzzle Size | Sequential | Parallel (8 cores) | Recommendation |
|-------------|-----------|-------------------|----------------|
| 3×3 | 0.03s | 0.04s | Sequential |
| 4×4 | 0.2s | 0.08s | Either |
| 5×5 | 5s | 0.8s | Parallel |
| 6×6+ | Minutes | Seconds | Parallel |

**Rule of thumb**: Use parallel mode when estimated solve time > 5 seconds

### Thread Pool Tuning

The solver uses ForkJoinPool. Advanced tuning:

```bash
# Increase parallelism for work-stealing
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8

# Adjust work-stealing queue size
-Djava.util.concurrent.ForkJoinPool.common.maximumSpares=4
```

## Memory Management

### Memory Usage Patterns

| Component | Memory per Cell | Typical 6×6 Puzzle |
|-----------|----------------|-------------------|
| Board state | ~100 bytes | ~3.6 KB |
| Domain storage | ~50 bytes | ~1.8 KB |
| Edge tables | ~1 KB | ~1 KB |
| Thread clones | Board × threads | ~30 KB (8 threads) |

### Reducing Memory Usage

#### 1. Disable Verbose Output

```bash
# Quiet mode saves memory on string building
java -jar eternity-solver.jar -q puzzle
```

#### 2. Limit Parallel Threads

```bash
# Fewer threads = fewer board clones
java -jar eternity-solver.jar -p -t 4 puzzle
```

#### 3. Use Smaller Heap

```bash
# Force tighter memory management
java -Xmx512M -jar eternity-solver.jar puzzle
```

#### 4. Disable Statistics

```bash
# Reduces tracking overhead (not currently exposed in CLI)
# Future option: --no-statistics
```

### Memory Profiling

```bash
# Enable native memory tracking
java -XX:NativeMemoryTracking=summary \
     -jar eternity-solver.jar puzzle

# View memory usage
jcmd <PID> VM.native_memory summary
```

## Algorithm Selection

### Optimization Toggles

```bash
# Baseline (all optimizations enabled)
java -jar eternity-solver.jar example_4x4

# Disable singleton detection
java -jar eternity-solver.jar --no-singletons example_4x4

# Prioritize border filling (MRV variant)
java -jar eternity-solver.jar --prioritize-borders example_4x4

# Use descending piece order
java -jar eternity-solver.jar --order descending example_4x4
```

### When to Disable Optimizations

#### Singleton Detection

**Disable when**:
- Puzzles with very few constraints
- Memory is extremely limited
- You want deterministic search order

**Command**:
```bash
java -jar eternity-solver.jar --no-singletons puzzle
```

#### Border Prioritization

**Enable when**:
- Puzzle has strong border constraints
- Many pieces have gray edges
- Sequential solving

**Command**:
```bash
java -jar eternity-solver.jar --prioritize-borders puzzle
```

### Piece Ordering Strategies

```bash
# Ascending (default): smallest ID first
java -jar eternity-solver.jar --order ascending puzzle

# Descending: largest ID first
java -jar eternity-solver.jar --order descending puzzle

# Difficulty-based: hardest pieces first (future)
# java -jar eternity-solver.jar --order difficulty puzzle
```

## Profiling and Monitoring

### Built-in Statistics

Enable verbose mode to see detailed statistics:

```bash
java -jar eternity-solver.jar -v puzzle
```

**Output includes**:
- Total recursive calls
- Backtrack count
- Singleton placements
- Domain reductions
- Time per optimization phase

### JVM Profiling Tools

#### 1. Java Flight Recorder (JFR)

```bash
# Record performance data
java -XX:StartFlightRecording=duration=60s,filename=profile.jfr \
     -jar eternity-solver.jar puzzle

# Analyze with JDK Mission Control
jmc profile.jfr
```

#### 2. VisualVM

```bash
# Run solver with JMX enabled
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar eternity-solver.jar puzzle

# Connect VisualVM to localhost:9010
```

#### 3. Async Profiler

```bash
# Download async-profiler
# https://github.com/jvm-profiling-tools/async-profiler

# Run profiler
./profiler.sh -d 30 -f flamegraph.html <PID>
```

### Performance Metrics

#### Key Metrics to Monitor

| Metric | Tool | Target |
|--------|------|--------|
| CPU utilization | top/htop | 80-95% |
| Memory usage | jstat | < 80% of heap |
| GC pause time | GC logs | < 200ms |
| Thread count | jstack | = configured threads |
| Backtrack rate | Verbose output | Decreasing over time |

#### Using jstat

```bash
# Monitor GC activity
jstat -gc <PID> 1000

# Monitor memory
jstat -gcutil <PID> 1000
```

## Benchmarking

### Creating Reproducible Benchmarks

#### 1. Warm-up the JVM

```bash
# Run a smaller puzzle first
java -jar eternity-solver.jar example_3x3
java -jar eternity-solver.jar example_4x4

# Then run your benchmark
java -jar eternity-solver.jar puzzle_6x6
```

#### 2. Use Consistent Settings

```bash
# Fix thread count
java -jar eternity-solver.jar -p -t 8 puzzle

# Fix heap size
java -Xmx4G -jar eternity-solver.jar puzzle

# Disable adaptive sizing
java -Xms4G -Xmx4G -XX:-UseAdaptiveSizePolicy \
     -jar eternity-solver.jar puzzle
```

#### 3. Multiple Runs

```bash
# Bash script for multiple runs
for i in {1..5}; do
    echo "Run $i:"
    time java -jar eternity-solver.jar puzzle
done
```

### Benchmark Script

```bash
#!/bin/bash
# benchmark.sh

PUZZLE=$1
RUNS=${2:-5}
THREADS=${3:-8}

echo "Benchmarking $PUZZLE with $RUNS runs, $THREADS threads"
echo "=================================================="

TOTAL=0
for i in $(seq 1 $RUNS); do
    echo -n "Run $i: "

    START=$(date +%s%3N)
    java -Xmx4G -jar eternity-solver-1.0.0.jar -p -t $THREADS -q $PUZZLE > /dev/null
    END=$(date +%s%3N)

    DURATION=$((END - START))
    TOTAL=$((TOTAL + DURATION))

    echo "${DURATION}ms"
done

AVG=$((TOTAL / RUNS))
echo "=================================================="
echo "Average: ${AVG}ms"
```

**Usage**:
```bash
chmod +x benchmark.sh
./benchmark.sh example_4x4 10 8
```

### Comparative Benchmarking

```bash
# Sequential vs Parallel
time java -jar eternity-solver.jar puzzle
time java -jar eternity-solver.jar -p -t 8 puzzle

# With/without optimizations
time java -jar eternity-solver.jar puzzle
time java -jar eternity-solver.jar --no-singletons puzzle
```

## Hardware Recommendations

### CPU

**Optimal**: Modern multi-core CPU with high single-thread performance
- Intel: i7/i9 9th gen or newer
- AMD: Ryzen 7/9 3000 series or newer
- Apple: M1/M2/M3

**Key factors**:
1. **Single-thread performance**: Critical for sequential and early parallel phases
2. **Core count**: 6-16 cores ideal for parallel mode
3. **Cache size**: Larger L3 cache improves performance (16+ MB recommended)

### Memory

**Minimum**: 4 GB RAM
**Recommended**: 8-16 GB RAM
**Optimal**: 32 GB RAM for 16×16 puzzles

**Characteristics**:
- Speed: DDR4-3200 or faster
- Latency: Lower CL (CAS latency) is better

### Storage

Not critical for solving, but faster storage helps for:
- Loading puzzle files
- Writing save files
- JVM startup

**Recommended**: SSD (SATA or NVMe)

### Operating System

**Performance ranking**:
1. Linux (best): Least overhead, better thread scheduling
2. macOS: Good performance, especially on Apple Silicon
3. Windows: Good, but slightly higher OS overhead

**Recommendations**:
- Use 64-bit OS
- Disable unnecessary background services
- Use performance power plan (Windows) or disable power management (Linux)

## Platform-Specific Tuning

### Linux

```bash
# Increase file descriptor limit
ulimit -n 65536

# Use transparent huge pages
echo always > /sys/kernel/mm/transparent_hugepage/enabled

# Set CPU governor to performance
sudo cpupower frequency-set -g performance
```

### macOS

```bash
# Increase max processes
sysctl -w kern.maxproc=2048

# Increase max files
sysctl -w kern.maxfiles=65536
```

### Windows

```powershell
# Set high-performance power plan
powercfg /setactive 8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c

# Increase priority (run as Administrator)
wmic process where name="java.exe" CALL setpriority "high priority"
```

## Troubleshooting Performance Issues

### Problem: Slow Startup

**Symptoms**: Takes several seconds before solving begins

**Solutions**:
1. Reduce heap initialization: Remove `-Xms` flag
2. Use class data sharing: `java -Xshare:auto`
3. Warm up JIT: Run smaller puzzle first

### Problem: Excessive Memory Usage

**Symptoms**: OutOfMemoryError, high swap usage

**Solutions**:
1. Increase heap: `-Xmx8G`
2. Reduce threads: `-t 4`
3. Use quiet mode: `-q`
4. Enable G1 GC: `-XX:+UseG1GC`

### Problem: Poor Parallel Scaling

**Symptoms**: 8 threads only 2x faster than 1 thread

**Possible causes**:
1. Puzzle too small (< 5×5)
2. CPU thermal throttling
3. Insufficient memory bandwidth
4. Hyperthreading overhead

**Solutions**:
1. Use sequential mode for small puzzles
2. Monitor CPU temperatures
3. Reduce thread count to physical cores
4. Disable hyperthreading in BIOS

### Problem: Long GC Pauses

**Symptoms**: Periodic freezes, "GC overhead limit exceeded"

**Solutions**:
1. Increase heap size: `-Xmx8G`
2. Use G1 GC: `-XX:+UseG1GC`
3. Tune GC pause time: `-XX:MaxGCPauseMillis=100`
4. Reduce allocation rate (use quiet mode)

## Performance Checklist

### Before Benchmarking

- [ ] Warm up JVM with smaller puzzle
- [ ] Close unnecessary applications
- [ ] Disable OS power management
- [ ] Ensure adequate cooling
- [ ] Use consistent JVM flags
- [ ] Multiple runs for statistical significance

### Production Deployment

- [ ] Set appropriate heap size (`-Xmx`)
- [ ] Enable G1 GC for large heaps
- [ ] Configure thread count for hardware
- [ ] Enable GC logging for monitoring
- [ ] Use quiet mode to reduce overhead
- [ ] Monitor with JMX/VisualVM

### Optimization Order

1. **Measure baseline** (default settings)
2. **Enable parallel mode** if puzzle ≥ 5×5
3. **Tune thread count** (test 4, 6, 8, 12)
4. **Optimize heap size** (start with 2-4GB)
5. **Tune GC** if pauses > 200ms
6. **Profile hotspots** if still slow
7. **Consider algorithm toggles** as last resort

## Summary of Common Configurations

### Quick Reference Table

| Use Case | Command |
|----------|---------|
| Default (all puzzles) | `java -jar eternity-solver.jar puzzle` |
| Small puzzles (≤4×4) | `java -Xmx512M -jar eternity-solver.jar puzzle` |
| Medium puzzles (5×5-6×6) | `java -Xmx2G -jar eternity-solver.jar -p -t 6 puzzle` |
| Large puzzles (≥7×7) | `java -Xmx4G -XX:+UseG1GC -jar eternity-solver.jar -p -t 8 puzzle` |
| Low memory | `java -Xmx256M -XX:+UseSerialGC -jar eternity-solver.jar -q puzzle` |
| Maximum performance | `java -Xmx8G -XX:+UseG1GC -XX:+AggressiveOpts -jar eternity-solver.jar -p -t 12 puzzle` |

## Further Reading

- [Algorithm Guide](ALGORITHM_GUIDE.md) - Understanding the optimizations
- [API Reference](API_REFERENCE.md) - Integrating the solver as a library
- [JVM Performance Tuning](https://docs.oracle.com/en/java/javase/11/gctuning/)
- [Concurrent Programming in Java](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
