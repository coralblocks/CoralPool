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
	private final List<SoftReference<E[]>> oldArrays = new ArrayList<SoftReference<E[]>>();
	
	@SuppressWarnings("unchecked")
	public ArrayObjectPool(int maxCapacity, int preloadCount, Builder<E> builder) {
		this.array = (E[]) new Object[maxCapacity];
		for(int i = 0; i < preloadCount; i++) {
			this.array[i] = builder.newInstance();
		}
		this.builder = builder;
	}

	@Override
	public final E get() {
		
		if (pointer == array.length) {

			int oldLength = array.length;
            int newLength = oldLength + (oldLength / 2);

            @SuppressWarnings("unchecked")
			E[] newArray = (E[]) new Object[newLength];
            
            oldArrays.add(new SoftReference<E[]>(this.array)); // delay gc
            
            this.array = newArray;
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
	public Builder<E> getBuilder() {
		return builder;
	}
}
