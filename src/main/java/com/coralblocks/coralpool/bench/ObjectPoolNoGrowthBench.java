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
import com.coralblocks.coralpool.ObjectBuilder;
import com.coralblocks.coralpool.ObjectPool;
import com.coralblocks.coralpool.StackObjectPool;
import com.coralblocks.coralpool.TieredObjectPool;

public class ObjectPoolNoGrowthBench {

	private static enum Type { LINKED, ARRAY, MULTI, STACK, TIERED }

	// Give the JIT time to compile the exercised paths before any samples contribute to the result.
	private static final int DEFAULT_WARMUP_PASSES = 50;
	
	private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");
	
	public static void main(String[] args) {
		
		final Type type = Type.valueOf(args[0].toUpperCase());
		final int initialCapacity = args.length > 1 ? Integer.parseInt(args[1]) : 2_000;
		final int preloadCount = args.length > 2 ? Integer.parseInt(args[2]) : initialCapacity;
		final int passes = args.length > 3 ? Integer.parseInt(args[3]) : 1_000;
		final int warmupPasses = args.length > 4 ? Integer.parseInt(args[4]) : DEFAULT_WARMUP_PASSES;
		if (passes <= 0) throw new IllegalArgumentException("Passes must be greater than zero: " + passes);
		if (warmupPasses < 0) throw new IllegalArgumentException("Warmup passes cannot be negative: " + warmupPasses);
		final int totalPasses = warmupPasses + passes;
		
		final Object[] builderObjects = createObjects(initialCapacity);
		final Object[] acquiredObjectsFromPool = new Object[initialCapacity];
		
		System.out.println();

		System.out.println("type=" + type + 
		           " initialCapacity=" + initialCapacity + 
		           " preloadCount=" + preloadCount +
		           " passes=" + passes +
		           " warmupPasses=" + warmupPasses +
		           "\n");
		
		long totalTime = 0;
		
		for(int y = 0; y < totalPasses; y++) {
			
			ObjectPool<Object> pool = createObjectPool(type ,initialCapacity, preloadCount, builderObjects);
		
			long start = System.nanoTime();
			
			for(int i = 1; i <= initialCapacity; i++) {
				for(int x = 0; x < i; x++) {
					acquiredObjectsFromPool[x] = pool.get();
				}
				for(int x = 0; x < i; x++) {
					pool.release(acquiredObjectsFromPool[x]);
				}
			}
			
			long time = System.nanoTime() - start;
			
			// Warmup samples exercise the same code but are excluded so JIT compilation does not skew the average.
			if (y >= warmupPasses) totalTime += time;

			if (y < warmupPasses) {
				System.out.print("\rWarmup pass: " + (y + 1) + "/" + warmupPasses);
			} else {
				if (y == warmupPasses && warmupPasses > 0) System.out.println();
				System.out.print("\rMeasured pass: " + (y - warmupPasses + 1) + "/" + passes);
			}
		}
		
		System.out.println("\n\n" + FORMATTER.format(totalTime / passes) + " nanoseconds (passes=" + passes + ")");
		System.out.println();
	}
	
	private static Object[] createObjects(int count) {
		Object[] objects = new Object[count];
		for(int i = 0; i < count; i++) {
			objects[i] = new Object();
		}
		return objects;
	}

	private static ObjectPool<Object> createObjectPool(Type type, int initialCapacity, int preloadCount, final Object[] builderObjects) {
		
		// Return distinct objects allocated before the benchmark to isolate pool mechanics without allocation latency.
		ObjectBuilder<Object> builder = new ObjectBuilder<Object>() {
			
			private int index = 0;

			@Override
			public Object newInstance() {
				return builderObjects[index++];
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
