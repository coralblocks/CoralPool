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

import java.text.DecimalFormat;

import com.coralblocks.coralpool.ArrayObjectPool;
import com.coralblocks.coralpool.LinkedObjectPool;
import com.coralblocks.coralpool.ObjectPool;
import com.coralblocks.coralpool.util.Builder;

public class ObjectPoolBench2 {

	private static enum Type { ARRAY, LINKED }
	
	private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");
	
	public static void main(String[] args) {
		
		final int initialCapacity = args.length > 0 ? Integer.parseInt(args[0]) : 100;
		final int preloadCount = args.length > 1 ? Integer.parseInt(args[1]) : initialCapacity / 2;
		
		final Object object = new Object();
		Builder<Object> builder = new Builder<Object>() {
			@Override
			public Object newInstance() {
				return object;
			}
		};
		
		System.out.println();

		for(Type type : Type.values()) {
		
			ObjectPool<Object> pool = null;
			if (type == Type.LINKED) {
				pool = new LinkedObjectPool<Object>(initialCapacity, preloadCount, builder);
			} else if (type == Type.ARRAY) {
				pool = new ArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
			} else {
				throw new IllegalArgumentException("Bad type: " + type);
			}
			
			System.out.println("type=" + pool.getClass().getSimpleName() + 
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
			
			System.out.println(FORMATTER.format(time) + " nanoseconds\n");
		}
	}
}