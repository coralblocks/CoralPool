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

public class LinkedObjectPool<E> implements ObjectPool<E> {
	
	private final LinkedObjectList<E> linkedList;
	private final Builder<E> builder;
	
	public LinkedObjectPool(int preloadCount, Builder<E> builder) {
		this.builder = builder;
		this.linkedList = new LinkedObjectList<E>(preloadCount * 2);
		for(int i = 0; i < preloadCount; i++) {
			linkedList.addLast(builder.newInstance());
		}
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

	@Override
	public Builder<E> getBuilder() {
		return builder;
	}
}