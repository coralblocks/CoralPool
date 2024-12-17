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

import com.coralblocks.coralpool.util.Builder;
import com.coralblocks.coralpool.util.LinkedObjectList;

/**
 * An {@link ObjectPool} backed by an internal linked-list. The pool can grow indefinitely by reclaiming new instances from the outside world, in other words,
 * you can {@link release(E)} new instances back to a full pool and the pool will grow to accommodate the new instances. You can also keep calling {@link get()} on
 * an empty pool and the pool will allocate through its {@link Builder} a new instance to be returned. Basically the pool can never return a <code>null</code>
 * object through its {@link get()} method.
 *
 * @param <E> the object being served by this object pool
 */
public class LinkedObjectPool<E> implements ObjectPool<E> {
	
	private final LinkedObjectList<E> linkedList;
	private final Builder<E> builder;
	
	/**
	 * Creates a new <code>LinkedObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param klass the class used as the builder of the pool
	 */
	public LinkedObjectPool(int initialCapacity, Class<E> klass) {
		this(initialCapacity, Builder.createBuilder(klass));
	}	
	
	/**
	 * Creates a new <code>LinkedObjectPool</code> with the given initial capacity. The entire pool (its entire initial capacity) will be populated 
	 * with new instances at startup, in other words, the <code>preloadCount</code> is assumed to the same as the <code>initialCapacity</code>.  
	 * 
	 * @param initialCapacity the initial capacity of the pool
	 * @param builder the builder of the pool
	 */
	public LinkedObjectPool(int initialCapacity, Builder<E> builder) {
		this(initialCapacity, initialCapacity, builder);
	}
	
	/**
	 * Creates a new <code>LinkedObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param klass the class used as the builder of the pool
	 */
	public LinkedObjectPool(int initialCapacity, int preloadCount, Class<E> klass) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass));
	}
	
	/**
	 * Creates a new <code>LinkedObjectPool</code> with the given initial capacity. The pool will be populated with the given preload count,
	 * in other words, the pool will preallocate <code>preloadCount</code> instances at startup.
	 *   
	 * @param initialCapacity the initial capacity of the pool
	 * @param preloadCount the number of instances to preallocate at startup
	 * @param builder the builder of the pool
	 */
	public LinkedObjectPool(int initialCapacity, int preloadCount, Builder<E> builder) {
		check(initialCapacity, preloadCount);
		this.builder = builder;
		this.linkedList = new LinkedObjectList<E>(initialCapacity);
		for(int i = 0; i < preloadCount; i++) {
			linkedList.addLast(builder.newInstance());
		}
	}
	
	private void check(int initialCapacity, int preloadCount) {
		if (preloadCount > initialCapacity) {
			throw new IllegalArgumentException("preloadCount (" + preloadCount + ") cannot be bigger than initialCapacity (" + initialCapacity + ")");
		}
	}
	
	int getLinkedListSize() {
		return this.linkedList.size();
	}

	@Override
	public E get() {
		E toReturn = linkedList.removeLast();
		if (toReturn == null) {
			return builder.newInstance();
		}
		return toReturn;
	}

	@Override
	public void release(E e) {
		linkedList.addLast(e);
	}
}