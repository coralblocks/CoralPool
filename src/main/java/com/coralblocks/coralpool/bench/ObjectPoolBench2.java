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
package com.coralblocks.coralpool.bench;

import com.coralblocks.coralpool.ArrayObjectPool;
import com.coralblocks.coralpool.LinkedObjectPool;
import com.coralblocks.coralpool.ObjectPool;
import com.coralblocks.coralpool.util.Builder;

public class ObjectPoolBench2 {

	private static enum Type { LINKED, ARRAY }
	
	public static void main(String[] args) {
		
		final Type type = getType(args);
		final int initialCapacity = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		final int preloadCount = args.length > 2 ? Integer.parseInt(args[2]) : initialCapacity / 2;
		
		final Object object = new Object();
		Builder<Object> builder = new Builder<Object>() {
			@Override
			public Object newInstance() {
				return object;
			}
		};
		
		ObjectPool<Object> pool = null;
		if (type == Type.LINKED) {
			pool = new LinkedObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.ARRAY) {
			pool = new ArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else {
			throw new IllegalArgumentException("Bad type: " + type);
		}
		
		System.out.println("\ntype=" + pool.getClass().getSimpleName() + 
						   " initialCapacity=" + initialCapacity + " preloadCount=" + preloadCount + "\n");
		
		long start = System.nanoTime();
		
		Object obj = null;
		
		for(int i = 1; i <= initialCapacity; i++) {
			for(int x = 0; x < i; x++) {
				obj = pool.get();
			}
			for(int x = 0; x < i; x++) {
				pool.release(obj);
			}
		}
		
		long time = System.nanoTime() - start;
		
		System.out.println(time + " nanoseconds\n");
	}
	
	private static Type getType(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("Pool type must be provided! (LinkedObjectPool or ArrayObjectPool)");
		}
		String type = args[0];
		if (type.equalsIgnoreCase(ArrayObjectPool.class.getSimpleName())) return Type.ARRAY;
		if (type.equalsIgnoreCase(LinkedObjectPool.class.getSimpleName())) return Type.LINKED;
		throw new IllegalArgumentException("Unrecognizable pool implementation: " + type);
	}
	
}