# CoralPool
Multiple _fast_ implementations of object pooling classes designed to efficiently reuse mutable objects, reducing memory allocations and eliminating the garbage collector _overhead_.

## ObjectPool Interface
```java
public interface ObjectPool<E> {

    public E get();

    public void release(E e);
}
```

#### Example:
```java
// the pool can grow, but it will start with 100 slots
final int initialCapacity = 100;

// the pool can allocate more instances later, but it will start with 50 instances
final int preloadCount = 50;

// the pool can use a Builder, but it can also take a Class for creating instances through
// the default constructor
final Class<StringBuilder> klass = StringBuilder.class;

// Create your object pool
ObjectPool<StringBuilder> pool = new LinkedObjectPool<>(initialCapacity, preloadCount, klass);

// Fetch an instance from the pool
StringBuilder sb = pool.get();

// Do whatever you want with the instance
// (...)

// When you are done return the instance back to the pool
pool.release(sb);
```

## LinkedObjectPool

An `ObjectPool` backed by an internal linked-list. The pool can grow indefinitely by adding new nodes to the list. You can call `get()` forever and the pool will keep returning newly allocated instances through its internal `Builder<E>`. Basically the pool can never return a `null` object through its `get()` method.

You can also add new instances from external sources, that is, instances not created by the pool, using the `release(E)` method.
If the pool is full when you call `release(E)`, it will expand the underlying linked-list by adding a new node to accommodate the instance.

## ArrayObjectPool

An `ObjectPool` backed by an internal array. The pool can grow indefinitely, allowing you to continuously call `get()` to receive new instances. When the pool runs out of instances, the internal array grows to accommodate more. Essentially, the pool will never return a `null` object through its `get()` method.

You can also add instances from external sources, that is, instances not created by the pool, using the `release(E)` method. If the pool is full when you call `release(E)`, it will grow to accommodate the new instance.

When the pool grows, a larger internal array is allocated. Instead of discarding the previous array, it is stored as a [SoftReference](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/ref/SoftReference.html) to delay garbage collection. A `SoftReference` allows the JVM to postpone garbage collection until memory is critically low, which should rarely occur. If needed, you can manually release these soft references by calling the `releaseSoftReferences()` method from `ArrayObjectPool`.

## Benchmarks

As detailed above, `ArrayObjectPool` has the drawback of having to allocate a new array to grow, but it is slightly faster than the `LinkedObjectPool`. You can find the benchmarks [here](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/bench/ObjectPoolBench1.java) and [here](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/bench/ObjectPoolBench2.java). We have used <a href="https://www.github.com/coralblocks/CoralBench" target="_blank">CoralBench</a> for the benchmarks. Below the results:

<details>
  <summary> ObjectPoolBenchmark1:</summary>

<br/>

```
$ java -verbose:gc -XX:+AlwaysPreTouch -Xms4g -Xmx4g -XX:NewSize=512m \
        -XX:MaxNewSize=1024m -cp target/classes:target/coralpool-all.jar \
        com.coralblocks.coralpool.bench.ObjectPoolBench1 1000000 5000000
[0.022s][info][gc] Using G1

type=ArrayObjectPool warmup=1000000 measurements=5000000

GET:
Measurements: 5,000,000 | Warm-Up: 1,000,000 | Iterations: 6,000,000
Avg Time: 17.410 nanos | Min Time: 14.000 nanos | Max Time: 26.294 micros
75% = [avg: 16.000 nanos, max: 18.000 nanos]
90% = [avg: 16.000 nanos, max: 19.000 nanos]
99% = [avg: 17.000 nanos, max: 21.000 nanos]
99.9% = [avg: 17.000 nanos, max: 30.000 nanos]
99.99% = [avg: 17.000 nanos, max: 110.000 nanos]
99.999% = [avg: 17.000 nanos, max: 1.821 micros]

RELEASE:
Measurements: 5,000,000 | Warm-Up: 1,000,000 | Iterations: 6,000,000
Avg Time: 17.410 nanos | Min Time: 14.000 nanos | Max Time: 33.848 micros
75% = [avg: 16.000 nanos, max: 19.000 nanos]
90% = [avg: 16.000 nanos, max: 19.000 nanos]
99% = [avg: 17.000 nanos, max: 21.000 nanos]
99.9% = [avg: 17.000 nanos, max: 29.000 nanos]
99.99% = [avg: 17.000 nanos, max: 78.000 nanos]
99.999% = [avg: 17.000 nanos, max: 1.367 micros]

type=LinkedObjectPool warmup=1000000 measurements=5000000

GET:
Measurements: 5,000,000 | Warm-Up: 1,000,000 | Iterations: 6,000,000
Avg Time: 25.000 nanos | Min Time: 16.000 nanos | Max Time: 22.628 micros
75% = [avg: 24.000 nanos, max: 25.000 nanos]
90% = [avg: 24.000 nanos, max: 25.000 nanos]
99% = [avg: 24.000 nanos, max: 34.000 nanos]
99.9% = [avg: 24.000 nanos, max: 146.000 nanos]
99.99% = [avg: 24.000 nanos, max: 257.000 nanos]
99.999% = [avg: 24.000 nanos, max: 723.000 nanos]

RELEASE:
Measurements: 5,000,000 | Warm-Up: 1,000,000 | Iterations: 6,000,000
Avg Time: 27.020 nanos | Min Time: 17.000 nanos | Max Time: 25.742 micros
75% = [avg: 26.000 nanos, max: 27.000 nanos]
90% = [avg: 26.000 nanos, max: 28.000 nanos]
99% = [avg: 26.000 nanos, max: 29.000 nanos]
99.9% = [avg: 26.000 nanos, max: 155.000 nanos]
99.99% = [avg: 26.000 nanos, max: 267.000 nanos]
99.999% = [avg: 26.000 nanos, max: 737.000 nanos]
```
</details>

```
ArrayObjectPool     get()   => Avg: 17 ns | Min: 14 ns | 99.9% = [avg: 17 ns, max: 30 ns]
                 release(E) => Avg: 17 ns | Min: 14 ns | 99.9% = [avg: 17 ns, max: 29 ns]

LinkedObjectPool    get()   => Avg: 25 ns | Min: 16 ns | 99.9% = [avg: 24 ns, max: 146 ns]
                 release(E) => Avg: 27 ns | Min: 17 ns | 99.9% = [avg: 26 ns, max: 155 ns]
```

<details>
  <summary> ObjectPoolBenchmark2:</summary>

<br/>

```
$ java -verbose:gc -XX:+AlwaysPreTouch -Xms4g -Xmx4g -XX:NewSize=512m \
        -XX:MaxNewSize=1024m -cp target/classes:target/coralpool-all.jar \
        com.coralblocks.coralpool.bench.ObjectPoolBench2 100
[0.024s][info][gc] Using G1

type=ArrayObjectPool initialCapacity=100 preloadCount=50

401,221 nanoseconds for 10100 calls

type=LinkedObjectPool initialCapacity=100 preloadCount=50

638,698 nanoseconds for 10100 calls
```
</details>

The latency difference is small (few nanoseconds) but if you call `get()` and `release(E)` thousands of times it can add up.
```
ArrayObjectPool   => 401,221 nanoseconds for 10100 calls

LinkedObjectPool  => 638,698 nanoseconds for 10100 calls
```
