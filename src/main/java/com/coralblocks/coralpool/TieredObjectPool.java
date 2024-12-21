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
import com.coralblocks.coralpool.util.LinkedObjectList;

public class TieredObjectPool<E> implements ObjectPool<E> {
	
	public static int LINKED_LIST_INITIAL_CAPACITY_FACTOR = 3;
	
	private E[] array;
	private int pointer = 0;
	private final Builder<E> builder;
	private final LinkedObjectList<E> linkedList;
	
	/**
	 * Creates a new <code>TieredObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param klass the class used as the builder of the pool
	 */
	public TieredObjectPool(int initialCapacity, Class<E> klass) {
		this(initialCapacity, Builder.createBuilder(klass));
	}	
	
	/**
	 * Creates a new <code>TieredObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param builder the builder of the pool
	 */
	public TieredObjectPool(int initialCapacity, Builder<E> builder) {
		this(initialCapacity, initialCapacity, builder);
	}
	
	/**
	 * Creates a new <code>TieredObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param klass the class used as the builder of the pool
	 */
	public TieredObjectPool(int initialCapacity, int preloadCount, Class<E> klass) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass));
	}
	
	/**
	 * Creates a new <code>TieredObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param builder the builder of the pool
	 */
	@SuppressWarnings("unchecked")
	public TieredObjectPool(int initialCapacity, int preloadCount, Builder<E> builder) {
		check(initialCapacity, preloadCount);
		this.array = (E[]) new Object[initialCapacity];
		for(int i = initialCapacity - preloadCount; i < initialCapacity; i++) {
			this.array[i] = builder.newInstance();
		}
		this.builder = builder;
		this.pointer = initialCapacity; // important
		this.linkedList = new LinkedObjectList<E>(initialCapacity * LINKED_LIST_INITIAL_CAPACITY_FACTOR);
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
	
	LinkedObjectList<E> getLinkedList() {
		return this.linkedList;
	}
	
	@Override
	public final E get() {
		
		if (pointer == 0) {
			E toReturn = linkedList.removeLast();
			if (toReturn == null) {
				toReturn = builder.newInstance();
			}
			return toReturn;
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
			linkedList.addLast(object);
		} else {
			this.array[pointer++] = object;
		}
	}
	
	private final void ensureNotNull(E object) {
		if (object == null) throw new IllegalArgumentException("Cannot release null!");
	}
}
