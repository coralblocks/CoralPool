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

public class MultiArrayObjectPool<E> implements ObjectPool<E> {
	
	static class ArrayHolder<E> {
		
		final int index;
		ArrayHolder<E> prev;
		final E[] array;
		ArrayHolder<E> next;
		
		ArrayHolder(final E[] array, int index) {
			this.array = array;
			this.index = index;
		}
		
		final int getIndex(int pointer, int arrayLength) {
			return Math.abs(pointer % arrayLength);
		}
	}
	
	private ArrayHolder<E> arrays;
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
		this.arrays = new ArrayHolder<E>(array, 0);
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
	
	ArrayHolder<E> getArrays() {
		return this.arrays;
	}
	
	private final ArrayHolder<E> grow(boolean trueForRightFalseForLeft) {
		
		ArrayHolder<E> newArrayHolder;
		
		if (trueForRightFalseForLeft) {
			E[] newArray = allocateArray(arrayLength, preloadCount);
			newArrayHolder = new ArrayHolder<E>(newArray, this.arrays.index + 1);
			newArrayHolder.prev = this.arrays;
			this.arrays.next = newArrayHolder;
		} else {
			E[] newArray = allocateArray(arrayLength); // all nulls
			newArrayHolder = new ArrayHolder<E>(newArray, this.arrays.index - 1);
			newArrayHolder.next = this.arrays;
			this.arrays.prev = newArrayHolder;
		}
		
		return newArrayHolder;
	}
	
	@Override
	public final E get() {
		
		// if we move to next array, zero is the first index of course
		int arrayIndex = 0;
		
		// first pointer from next array => (arrays.index + 1) * arrayLength;
		if (pointer == (arrays.index + 1) * arrayLength) {
			
			if (arrays.next != null) {
				arrays = arrays.next;
			} else {
				arrays = grow(true);
			}
			
			// arrayIndex = 0; // REDUNDANT
			
		} else {
			
			arrayIndex = arrays.getIndex(pointer++, arrayLength);
		}
		
		E toReturn = this.arrays.array[arrayIndex];
		if (toReturn == null) {
			toReturn = builder.newInstance();
		} else {
			this.arrays.array[arrayIndex] = null;
		}

		return toReturn;
	}
	
	@Override
	public final void release(E object) {
		
		// if we move to previous array, arrayLength - 1 is the first index of course
		int arrayIndex = arrayLength - 1;
		
		// first pointer from current array = arrays.index * arrayLength;
		if (pointer-- == arrays.index * arrayLength) {
			
			if (arrays.prev != null) {
				arrays = arrays.prev;
			} else {
				arrays = grow(false);
			}
			
			// arrayIndex = arrayLength - 1; // REDUNDANT
			
		} else {
		
			arrayIndex = arrays.getIndex(pointer, arrayLength);
		}
		
		this.arrays.array[arrayIndex] = object;
	}
}
