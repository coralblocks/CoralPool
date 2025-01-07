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
import java.util.Arrays;

import com.coralblocks.coralpool.util.Builder;
import com.coralblocks.coralpool.util.LinkedObjectList;

/**
 * <p>An {@link ObjectPool} backed by an internal stack (implemented with an array).
 * The pool can expand by reallocating a larger stack to accommodate more instances.</p>
 * 
 * <p><b>NOTE:</b> This {@link ObjectPool} is intentionally designed for <b>single-threaded systems</b>. 
 * It is <i>not</i> thread-safe and will fail if accessed concurrently by multiple threads. 
 * If you require concurrent access, you must implement your own synchronization, which will inevitably 
 * introduce a significant performance overhead. Most systems we work with are inherently single-threaded, 
 * making synchronization unnecessary and allowing for maximum performance.</p>
 * 
 * @param <E> the type of objects managed by this object pool
 */
public class StackObjectPool<E> implements ObjectPool<E> {
	
	/**
	 * The default growth factor to use if not specified
	 */
	public static final float DEFAULT_GROWTH_FACTOR = 1.75f;
	
	/*
	 * Our LinkedObjectList does not produce any garbage, not even when it grows
	 */
	private final static int SOFT_REFERENCE_LINKED_LIST_INITIAL_SIZE = 32;
	
	private E[] array;
	private int pointer = 0;
	private final Builder<E> builder;
	private final float growthFactor;
	private final LinkedObjectList<SoftReference<E[]>> oldArrays = new LinkedObjectList<SoftReference<E[]>>(SOFT_REFERENCE_LINKED_LIST_INITIAL_SIZE);
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param klass the class used as the builder of the pool
	 */
	public StackObjectPool(int initialCapacity, Class<E> klass) {
		this(initialCapacity, Builder.createBuilder(klass));
	}	
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param klass the class used as the builder of the pool
	 * @param growthFactor by how much the pool will grow when it has to grow
	 */
	public StackObjectPool(int initialCapacity, Class<E> klass, float growthFactor) {
		this(initialCapacity, Builder.createBuilder(klass), growthFactor);
	}
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param builder the builder of the pool
	 */
	public StackObjectPool(int initialCapacity, Builder<E> builder) {
		this(initialCapacity, initialCapacity, builder);
	}
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param builder the builder of the pool
	 * @param growthFactor by how much the pool will grow when it has to grow
	 */
	public StackObjectPool(int initialCapacity, Builder<E> builder, float growthFactor) {
		this(initialCapacity, initialCapacity, builder, growthFactor);
	}
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param klass the class used as the builder of the pool
	 */
	public StackObjectPool(int initialCapacity, int preloadCount, Class<E> klass) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass));
	}
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param klass the class used as the builder of the pool
	 * @param growthFactor by how much the pool will grow when it has to grow
	 */
	public StackObjectPool(int initialCapacity, int preloadCount, Class<E> klass, float growthFactor) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass), growthFactor);
	}
	
	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param builder the builder of the pool
	 */
	public StackObjectPool(int initialCapacity, int preloadCount, Builder<E> builder) {
		this(initialCapacity, preloadCount, builder, DEFAULT_GROWTH_FACTOR);
	}

	/**
	 * Creates a new <code>StackObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param builder the builder of the pool
	 * @param growthFactor by how much the pool will grow when it has to grow
	 */
	@SuppressWarnings("unchecked")
	public StackObjectPool(int initialCapacity, int preloadCount, Builder<E> builder, float growthFactor) {
		check(initialCapacity, preloadCount, growthFactor);
		this.growthFactor = growthFactor;
		this.array = (E[]) new Object[initialCapacity];
		for(int i = initialCapacity - preloadCount; i < initialCapacity; i++) {
			this.array[i] = builder.newInstance();
		}
		this.builder = builder;
		this.pointer = initialCapacity; // important
	}
	
	private void check(int initialCapacity, int preloadCount, float growthFactor) {
		if (preloadCount > initialCapacity) {
			throw new IllegalArgumentException("preloadCount (" + preloadCount + ") cannot be bigger than initialCapacity (" + initialCapacity + ")");
		}
		if (growthFactor <= 1) {
			throw new IllegalArgumentException("growthFactor (" + growthFactor + ") must be bigger than one");
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
	 * 
	 * @return the number of soft references released
	 */
	public final int releaseSoftReferences() {
		int toReturn = oldArrays.size();
		oldArrays.clear();
		return toReturn;
	}
	
    @SuppressWarnings("unchecked")
    private void grow() {
    	
		int newLength = (int) (growthFactor * array.length); // casting faster than rounding
		
		if (newLength == array.length) newLength++;
		
    	E[] newArray = (E[]) new Object[newLength];
    	
        System.arraycopy(array, 0, newArray, 0, array.length);
        Arrays.fill(array, null);
        
        oldArrays.addLast(new SoftReference<E[]>(this.array));
        
        this.array = newArray;
    }
	
	@Override
	public final E get() {
		
		if (pointer == 0) {
			return builder.newInstance();
		}
		
		E toReturn = this.array[--pointer];
		if (toReturn != null) {
			this.array[pointer] = null;
		} else {
			toReturn = builder.newInstance();
		}
		
		return toReturn;
	}
	
	@Override
	public final void release(E object) {
		
		ensureNotNull(object);
		
		if (pointer == array.length) {
			grow();
		}
		this.array[pointer++] = object;
	}
	
	private final void ensureNotNull(E object) {
		if (object == null) throw new IllegalArgumentException("Cannot release null!");
	}
}
