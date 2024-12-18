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

// the pool can use a Builder, but it can also take a Class for creating instances through the default constructor
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

An `ObjectPool` backed by an internal linked-list. The pool can grow indefinitely by reclaiming new instances from the outside world, in other words,
you can `release(E)` new instances back to a full pool and the pool will grow to accommodate the external new instances. You can also keep calling `get()`
forever on an empty pool and the pool will allocate new instances through its internal `Builder<E>`. Basically the pool can never return a `null`
object through its `get()` method.

## ArrayObjectPool

An `ObjectPool` backed by an internal array. The pool can grow indefinitely by serving new instances to the outside world, in other words,
you can keep calling `get()` forever to receive new instances. When the pool gets empty the internal array will grow to accommodate more instances.
Basically the pool can never return a `null` object through its `get()` method.

Note that releasing an instance back to a full pool, _for the case that an extra instance was created externally_ and it is now being pushed into the pool, causes the
instance to be ignored and stored as a [SoftReference](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/ref/SoftReference.html). This delays the instance from being garbage collected.
Ideally you should never have to create an extra instance externally, in other words, if you need a new instance you should ask the pool for one instead of creating one yourself and later attempting to release it to the pool.
 
Also note that when the pool grows, a new larger internal array is allocated. Instead of discarding the previous one, it is also stored as a `SoftReference` to
delay it from being garbage collected. A `SoftReference` postpones the GC activity until the JVM is running out of memory, which should never happen.
If you want you can release the soft references to the GC through the public method `releaseSoftReferences()` of `ArrayObjectPool`.

## Benchmarks

As detailed above, `ArrayObjectPool` has some drawbacks when compared to `LinkedObjectPool`, but it is slightly faster. You can find the benchmarks [here](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/bench/ObjectPoolBench1.java) and [here](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/bench/ObjectPoolBench2.java). Below the results:

### ObjectPoolBenchmark1:
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
### ObjectPoolBenchmark2:
The latency difference is small (few nanoseconds) but if you call `get()` and `release(E)` a thounsand times it can add up.
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
