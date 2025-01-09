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
 * <p>An {@link ObjectPool} backed by two tiers: an internal array and a linked-list.
 * The pool can grow by adding new instances to the linked-list (second tier) so that the array (first tier) never has to grow.</p>
 * 
 * <p><b>NOTE:</b> This {@link ObjectPool} is intentionally designed for <b>single-threaded systems</b>. 
 * It is <i>not</i> thread-safe and will fail if accessed concurrently by multiple threads. 
 * If you require concurrent access, you must implement your own synchronization, which will inevitably 
 * introduce a significant performance overhead. Most systems we work with are inherently single-threaded, 
 * making synchronization unnecessary and allowing for maximum performance.</p>
 * 
 * @param <E> the type of objects managed by this object pool
 */
public class TieredObjectPool<E> implements ObjectPool<E> {
	
	/**
	 * The initial size of the linked-list used for the expansion of the pool (second tier) as a factor of the initial capacity of the pool
	 */
	public static int LINKED_LIST_CAPACITY_FACTOR = 3;
	
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
		for(int i = 0; i < preloadCount; i++) {
			this.array[i] = builder.newInstance();
		}
		this.builder = builder;
		this.linkedList = new LinkedObjectList<E>(initialCapacity * LINKED_LIST_CAPACITY_FACTOR);
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
		
		if (!linkedList.isEmpty()) { // always return from the list first
			return linkedList.removeLast();
		} else if (pointer == array.length) { // array also has nothing
			return builder.newInstance();
		}
		
		E toReturn = this.array[pointer];
		if (toReturn == null) {
			toReturn = builder.newInstance(); // might need to populate due to preloadCount < initialCapacity
		} else {
			this.array[pointer] = null; // nullify on disposal (always)
		}
		pointer++;
		return toReturn;
	}
	
	@Override
	public final void release(E object) {
		
		ensureNotNull(object);
		
		if (pointer > 0) { // always return to the array first, if there is space
			this.array[--pointer] = object;
		} else { // if not then return to linked-list
			linkedList.addLast(object);
		}
	}
	
	private final void ensureNotNull(E object) {
		if (object == null) throw new IllegalArgumentException("Cannot release null!");
	}
}
