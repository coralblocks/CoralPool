/* 
 * Copyright 2024 (c) CoralBlocks - http://www.coralblocks.com
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
 * <p>An {@link ObjectPool} backed by an internal array. The pool can grow indefinitely by serving new instances to the outside world, in other words,
 * you can keep calling {@link get()} to receive new instances until the pool is empty. When that happens the internal array will grow to accommodate newly created instances.
 * Basically the pool can never return a <code>null</code> object through its {@link get()} method.</p>
 * 
 * <p>Note that releasing an instance back to a full pool, for the case that the instance was created externally and it is now pushed into the pool, causes the
 * instance to be ignored and stored as a {@link java.lang.ref.SoftReference}. This delays the instance from being garbage collected. Ideally you should never have to create
 * an instance externally, in other words, if you need a new instance you should ask the pool for one instead of creating one yourself and later attempting to
 * release it back to the pool.</p>
 * 
 *  <p>Also note that when the pool grows, a new larger internal array is allocated. The previous one is also stored as a {@link java.lang.ref.SoftReference} to
 *  delay it from being garbage collected. A {@link java.lang.ref.SoftReference} postpones the GC activity until the JVM runs out of memory, which hopefully will
 *  never happen.</p>
 *
 * @param <E> the object being served by this object pool
 */
public class ArrayObjectPool<E> implements ObjectPool<E> {
	
	private E[] array;
	private int pointer = 0;
	private final Builder<E> builder;
	private final List<SoftReference<E[]>> oldArrays = new ArrayList<SoftReference<E[]>>(16);
	private final List<SoftReference<E>> discarded = new ArrayList<SoftReference<E>>(64);
	
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
	
	private final void grow() {
		
        int newLength = array.length + (array.length / 2);

        @SuppressWarnings("unchecked")
		E[] newArray = (E[]) new Object[newLength];
        
        oldArrays.add(new SoftReference<E[]>(this.array)); // delay gc
        
        this.array = newArray;
	}
	
	@Override
	public final E get() {
		
		if (pointer == array.length) {
			grow();
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
		if (pointer > 0) {
			this.array[--pointer] = object;
		} else {
			SoftReference<E> soft = new SoftReference<E>(object);
			discarded.add(soft);
		}
	}

	@Override
	public Builder<E> getBuilder() {
		return builder;
	}
}
