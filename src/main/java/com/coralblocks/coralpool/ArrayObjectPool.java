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

public class ArrayObjectPool<E> implements ObjectPool<E> {
	
	private E[] array;
	private int pointer = 0;
	private final Builder<E> builder;
	private final List<SoftReference<E[]>> oldArrays = new ArrayList<SoftReference<E[]>>(16);
	private final List<SoftReference<E>> discarded = new ArrayList<SoftReference<E>>(64);
	
	public ArrayObjectPool(int initialCapacity, Class<E> klass) {
		this(initialCapacity, Builder.createBuilder(klass));
	}	
	
	public ArrayObjectPool(int initialCapacity, Builder<E> builder) {
		this(initialCapacity, initialCapacity, builder);
	}
	
	public ArrayObjectPool(int initialCapacity, int preloadCount, Class<E> klass) {
		this(initialCapacity, preloadCount, Builder.createBuilder(klass));
	}

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
