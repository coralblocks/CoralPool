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
package com.coralblocks.coralpool.bench;

import java.text.DecimalFormat;

import com.coralblocks.coralpool.ArrayObjectPool;
import com.coralblocks.coralpool.LinkedObjectPool;
import com.coralblocks.coralpool.MultiArrayObjectPool;
import com.coralblocks.coralpool.ObjectPool;
import com.coralblocks.coralpool.StackObjectPool;
import com.coralblocks.coralpool.TieredObjectPool;
import com.coralblocks.coralpool.util.Builder;

public class ObjectPoolNoGrowthBench2 {

	private static enum Type { LINKED, ARRAY, MULTI, STACK, TIERED }
	
	private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");
	
	public static void main(String[] args) {
		
		final Type type = Type.valueOf(args[0].toUpperCase());
		final int initialCapacity = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		final int preloadCount = args.length > 2 ? Integer.parseInt(args[2]) : initialCapacity;
		
		final Object object = new Object();
		
		System.out.println();

		System.out.println("type=" + type + " initialCapacity=" + initialCapacity + " preloadCount=" + preloadCount + "\n");
		
		for(int y = 0; y < 2; y++) { // two passes (the first is to warmup)
			
			ObjectPool<Object> pool = createObjectPool(type ,initialCapacity, preloadCount, object);
		
			int callCount = 0;
			
			long start = System.nanoTime();
			
			Object obj = null;
			
			for(int i = 1; i <= initialCapacity; i++) {
				for(int x = 0; x < i; x++) {
					obj = pool.get();
				}
				for(int x = 0; x < i; x++) {
					pool.release(obj);
				}
				callCount += 2 * i;
			}
			
			long time = System.nanoTime() - start;
			
			System.out.println(FORMATTER.format(time) + " nanoseconds for " + callCount + " calls");
		}
		
		System.out.println();
	}
	
	private static ObjectPool<Object> createObjectPool(Type type, int initialCapacity, int preloadCount, final Object object) {
		
		Builder<Object> builder = new Builder<Object>() {
			@Override
			public Object newInstance() {
				return object;
			}
		};
		
		if (type == Type.LINKED) {
			return new LinkedObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.ARRAY) {
			return new ArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.MULTI) {
			return new MultiArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.STACK) {
			return new StackObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.TIERED) {
			return new TieredObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else {
			throw new IllegalArgumentException("Bad type: " + type);
		}
	}
}