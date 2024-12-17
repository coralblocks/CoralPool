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

public class ArrayObjectPool<E> implements ObjectPool<E> {
	
	private final E[] array;
	private int pointer = 0;
	private final LeakListener<E> listener;
	private final Builder<E> builder;
	private int leaks = 0;
	
	@SuppressWarnings("unchecked")
	public ArrayObjectPool(int maxCapacity, int preloadCount, Builder<E> builder, LeakListener<E> listener) {
		this.array = (E[]) new Object[maxCapacity];
		for(int i = 0; i < preloadCount; i++) {
			this.array[i] = builder.newInstance();
		}
		this.builder = builder;
		this.listener = listener;
	}

	@Override
	public final E get() {
		if (pointer == array.length) {
			E toReturn = builder.newInstance();
			if (listener != null) {
				listener.onLeaked(this, toReturn);
			}
			leaks++;
			return toReturn;
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
		}
	}

	@Override
	public int getLeaks() {
		return leaks;
	}

	@Override
	public Builder<E> getBuilder() {
		return builder;
	}
}
