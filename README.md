# CoralPool

CoralPool is a high-performance, lightweight and garbage-free Java _object pool_ implementation. It efficiently reuses mutable objects, minimizing the creation of _short-lived_ objects that would otherwise burden the garbage collector. The pool can grow to accommodate more instances by internally allocating new objects via the `get()` method or by accepting external objects (not originally allocated by the pool) through the `release(E)` method.

<pre>
<b>Note:</b> For a discussion of developing garbage-free applications you should refer to <a href="https://youtu.be/bhzv6lJtuOs">this video</a>
</pre>

## ObjectPool Interface
```java
public interface ObjectPool<E> {

   /**
    * Retrieves an instance from this object pool. If no instances are currently available,
    * a new instance will be created, and the pool will grow in size if necessary to 
    * accommodate more instances. This method can never return null.
    * 
    * @return an instance from the pool
    */
    public E get();

   /**
    * Returns an instance to this object pool. If the pool has no available space 
    * to accommodate the instance, it will expand as needed. The pool can accept 
    * external instances that were not necessarily created by it. 
    * Passing null as the instance will result in an exception being thrown.
    * 
    * @param instance the instance to return to the pool
    * @throws IllegalArgumentException if the provided instance is null
    */
    public void release(E instance);
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
## ObjectPool Implementations

### LinkedObjectPool

An `ObjectPool` backed by an internal linked-list. It can gradually grow by adding new nodes to the list.

### ArrayObjectPool

An `ObjectPool` backed by an internal array. It can expand by allocating a larger array. When that happens, the previous array is retained as a [SoftReference](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/ref/SoftReference.html) to delay garbage collection as much as possible.
You can manually release these references by calling its `releaseSoftReferences()` public method.

### MultiArrayObjectPool

An `ObjectPool` backed by an internal doubly linked-list of arrays. It expands by adding new nodes, each containing a newly allocated array, to the linked-list.

### StackObjectPool

An `ObjectPool` backed by an internal stack, implemented with an array. It can expand by allocating a larger stack. When that happens, the previous array of the stack is retained as a [SoftReference](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/ref/SoftReference.html) to delay garbage collection as much as possible.
You can manually release these references by calling its `releaseSoftReferences()` public method.

### TieredObjectPool

An `ObjectPool` implementation utilizing a two-tier structure: an internal array and a linked-list.
It grows by adding new instances to the linked-list (second tier), ensuring the array (first tier) remains a fixed size and does not require expansion.

## Benchmarks

Ideally, an object pool should be configured at startup with a big enough initial capacity to avoid growth, maximizing performance. However, since growth cannot always be avoided, we conducted two benchmarks: one where the pool remains fixed in size and another where it grows. In the growth benchmark, the pool expands exclusively through additional `get()` calls, not by adding external instances via the `release(E)` method. Adding external instances, those not created internally by the pool, is rarely needed or desirable.

You can find the benchmarks [here](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/bench/ObjectPoolNoGrowthBench.java) (without growth) and [here](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/bench/ObjectPoolGrowthBench.java) (with growth). Below the results:

<pre>
<b>Note:</b> We used the new <a href="https://stackoverflow.com/questions/79275170/why-does-this-simple-and-small-java-code-runs-30x-faster-in-all-graal-jvms-but-n"><b>JVMCI</b> JIT</a> (from Graal 23) for the benchmarks below
</pre>

### Benchmark (_without_ growth)

<pre>
LinkedObjectPool        => 9,698,254 nanoseconds
<b>ArrayObjectPool         => 3,658,073 nanoseconds</b>     
MultiArrayObjectPool    => 3,900,745 nanoseconds
StackObjectPool         => 3,813,075 nanoseconds
TieredObjectPool        => 3,786,247 nanoseconds                   
</pre>

The results above are expected as native arrays are faster than linked lists because they store elements in contiguous memory, enhancing cache locality and sequential access. In contrast, linked lists use scattered references across memory, leading to more cache misses and slower traversal time due to the overhead of following pointers.

### Benchmark (_with_ growth)

<pre>
LinkedObjectPool        => 241,073 microseconds
<b>ArrayObjectPool         => 87,784 microseconds</b>     
MultiArrayObjectPool    => 162,697 microseconds
StackObjectPool         => 129,899 microseconds
TieredObjectPool        => 237,377 microseconds                   
</pre>

The above results are expected because `TieredObjectPool` uses a linked list for growth, leading to the same cache miss inefficiencies observed in the previous benchmark _without_ growth. In contrast, `ArrayObjectPool` clearly outperforms all others, as its [growth implementation](https://github.com/coralblocks/CoralPool/blob/main/src/main/java/com/coralblocks/coralpool/ArrayObjectPool.java#L187) is more efficient by avoiding both array copying and nulling.

<pre>
<b>Note:</b> All benchmarks were executed with <i>-verbose:gc</i> to make sure no GC activity ever takes place
</pre>
