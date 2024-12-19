/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralpool;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import com.coralblocks.coralpool.util.Builder;

/**
 *  <p>An {@link ObjectPool} backed by an internal array. The pool doubles its size with each expansion, allowing you to continuously call {@link get()} to 
 *  receive new instances. Essentially, the pool will never return a <code>null</code> object through its {@link get()} method.</p>
 *  
 *  <p>You can also add instances from external sources, that is, instances not created by the pool, using the {@link release(E)} method. If the pool is 
 *  full when you call {@link release(E)}, it will grow to accommodate the new instance.</p>
 *  
 *  <p>When the pool grows, a larger internal array is allocated. Instead of discarding the previous array, it is stored 
 *  as a {@link java.lang.ref.SoftReference} to delay garbage collection. A <code>SoftReference</code> allows the JVM to postpone garbage collection until 
 *  memory is critically low, which should rarely occur. If needed, you can manually release these soft references by 
 *  calling the {@link releaseSoftReferences()} method from the pool.</p>
 *
 * @param <E> the object being served by this object pool
 */
public class ArrayObjectPool<E> implements ObjectPool<E> {
	
	private E[] array;
	private int pointer = 0;
	private final Builder<E> builder;
	private final List<SoftReference<E[]>> oldArrays = new ArrayList<SoftReference<E[]>>(16);
	
	/**
	 * Creates a new <code>ArrayObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param klass the class used as the builder of the pool
	 */
	public ArrayObjectPool(int initialCapacity, Class<E> klass) {
		this(initialCapacity, Builder.createBuilder(klass));
	}	
	
	/**
	 * Creates a new <code>ArrayObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param builder the builder of the pool
	 */
	public ArrayObjectPool(int initialCapacity, Builder<E> builder) {
		this(initialCapacity, initialCapacity, builder);
	}
	
	/**
	 * Creates a new <code>ArrayObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param klass the class used as the builder of the pool
	 */
	public ArrayObjectPool(int initialCapacity, int preloadCount, Class<E> klass) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass));
	}

	/**
	 * Creates a new <code>ArrayObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param builder the builder of the pool
	 */
	@SuppressWarnings("unchecked")
	public ArrayObjectPool(int initialCapacity, int preloadCount, Builder<E> builder) {
		check(initialCapacity, preloadCount);
		this.array = (E[]) new Object[initialCapacity];
		for(int i = 0; i < preloadCount; i++) {
			this.array[i] = builder.newInstance();
		}
		this.builder = builder;
	}
	
	private void check(int initialCapacity, int preloadCount) {
		if (preloadCount > initialCapacity) {
			throw new IllegalArgumentException("preloadCount (" + preloadCount + ") cannot be bigger than initialCapacity (" + initialCapacity + ")");
		}
	}
	
	int getArrayLength() {
		return this.array.length;
	}
	
	E getArrayElement(int index) {
		return this.array[index];
	}
	
	/**
	 * If the pool is holding on to references (to delay GC) through {@link java.lang.ref.SoftReference}s release them now to the GC.
	 */
	public final void releaseSoftReferences() {
		oldArrays.clear();
	}
	
	private final int grow(boolean copyAndShift) {

		// NOTE: don't change because of the nullifying logic below!
		// It must be AT LEAST double size
		int newLength = 2 * array.length;

		@SuppressWarnings("unchecked")
		E[] newArray = (E[]) new Object[newLength];
		
		int offset = this.array.length;

		if (copyAndShift) {
			offset = newArray.length - this.array.length; // place at the very end
			System.arraycopy(this.array, 0, newArray, offset, this.array.length);
			// NULLIFYING LOGIC HERE: (newArray will contain nulls at the front)
			System.arraycopy(newArray, 0, this.array, 0, this.array.length); // null out previous array
		} else {
			// No need to perform any copying here as the previous array will have only nulls!
		}

		oldArrays.add(new SoftReference<E[]>(this.array)); // delay gc

		this.array = newArray;
		
		return offset;
	}
	
	@Override
	public final E get() {
		
		if (pointer == array.length) {
			/*pointer = */grow(false); // pointer returned will be array length anyway!
		}
		
		E toReturn = this.array[pointer];
		if (toReturn == null) {
			toReturn = builder.newInstance();
		} else {
			this.array[pointer] = null;
		}
		pointer++;
		return toReturn;
	}
	
	@Override
	public final void release(E object) {
		if (pointer == 0) {
			pointer = grow(true);
		}
		this.array[--pointer] = object;
	}
}
