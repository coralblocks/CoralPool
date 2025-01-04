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

import com.coralblocks.coralpool.util.Builder;

/**
 * <p>An {@link ObjectPool} backed by an internal doubly linked-list of arrays.
 * The pool can grow by adding a new node (with a new allocated array) to the linked-list.</p>
 * 
 * <p><b>NOTE:</b> This {@link ObjectPool} is intentionally designed for <b>single-threaded systems</b>. 
 * It is <i>not</i> thread-safe and will fail if accessed concurrently by multiple threads. 
 * If you require concurrent access, you must implement your own synchronization, which will inevitably 
 * introduce a significant performance overhead. Most systems we work with are inherently single-threaded, 
 * making synchronization unnecessary and allowing for maximum performance.</p>
 * 
 * @param <E> the type of objects managed by this object pool
 */
public class MultiArrayObjectPool<E> implements ObjectPool<E> {
	
	static class ArrayHolder<E> {
		
		ArrayHolder<E> prev;
		final E[] array;
		ArrayHolder<E> next;
		
		ArrayHolder(final E[] array) {
			this.array = array;
		}
	}
	
	private ArrayHolder<E> arrayHolder;
	private int pointer = 0;
	private final Builder<E> builder;
	private final int preloadCount;
	private final int arrayLength;
	
	/**
	 * Creates a new <code>MultiArrayObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param klass the class used as the builder of the pool
	 */
	public MultiArrayObjectPool(int initialCapacity, Class<E> klass) {
		this(initialCapacity, Builder.createBuilder(klass));
	}	
	
	/**
	 * Creates a new <code>MultiArrayObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param builder the builder of the pool
	 */
	public MultiArrayObjectPool(int initialCapacity, Builder<E> builder) {
		this(initialCapacity, initialCapacity, builder);
	}
	
	/**
	 * Creates a new <code>MultiArrayObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param klass the class used as the builder of the pool
	 */
	public MultiArrayObjectPool(int initialCapacity, int preloadCount, Class<E> klass) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass));
	}

	/**
	 * Creates a new <code>MultiArrayObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param builder the builder of the pool
	 */
	public MultiArrayObjectPool(int initialCapacity, int preloadCount, Builder<E> builder) {
		check(initialCapacity, preloadCount);
		this.arrayLength = initialCapacity;
		this.preloadCount = preloadCount;
		this.builder = builder;
		E[] array = allocateArray(arrayLength, preloadCount);
		this.arrayHolder = new ArrayHolder<E>(array);
	}
	
	private final E[] allocateArray(int arrayLength, int preloadCount) {
		E[] array = allocateArray(arrayLength);
		for(int i = 0; i < preloadCount; i++) {
			array[i] = builder.newInstance();
		}
		return array;
	}
	
	@SuppressWarnings("unchecked")
	private final E[] allocateArray(int arrayLength) {
		return (E[]) new Object[arrayLength];
	}
	
	private final void check(int initialCapacity, int preloadCount) {
		if (preloadCount > initialCapacity) {
			throw new IllegalArgumentException("preloadCount (" + preloadCount + ") cannot be bigger than initialCapacity (" + initialCapacity + ")");
		}
	}
	
	ArrayHolder<E> getArrayHolder() {
		return this.arrayHolder;
	}
	
	private final ArrayHolder<E> grow(boolean trueForRightFalseForLeft) {
		
		ArrayHolder<E> newArrayHolder;
		
		if (trueForRightFalseForLeft) {
			E[] newArray = allocateArray(arrayLength, preloadCount);
			newArrayHolder = new ArrayHolder<E>(newArray);
			newArrayHolder.prev = this.arrayHolder;
			this.arrayHolder.next = newArrayHolder;
		} else {
			E[] newArray = allocateArray(arrayLength); // all nulls
			newArrayHolder = new ArrayHolder<E>(newArray);
			newArrayHolder.next = this.arrayHolder;
			this.arrayHolder.prev = newArrayHolder;
		}
		
		return newArrayHolder;
	}
	
	@Override
	public final E get() {
		
		if (pointer == arrayLength) {
			
			if (arrayHolder.next != null) {
				arrayHolder = arrayHolder.next;
			} else {
				arrayHolder = grow(true);
			}
			
			pointer = 0;
		}
			
		E toReturn = this.arrayHolder.array[pointer];
		if (toReturn == null) {
			toReturn = builder.newInstance();
		} else {
			this.arrayHolder.array[pointer] = null;
		}
		
		pointer++;

		return toReturn;
	}
	
	@Override
	public final void release(E object) {
		
		ensureNotNull(object);
		
		if (pointer == 0) {
			
			if (arrayHolder.prev != null) {
				arrayHolder = arrayHolder.prev;
			} else {
				arrayHolder = grow(false);
			}
			
			pointer = arrayLength;
		} 
		
		this.arrayHolder.array[--pointer] = object;
	}
	
	private final void ensureNotNull(E object) {
		if (object == null) throw new IllegalArgumentException("Cannot release null!");
	}
}
